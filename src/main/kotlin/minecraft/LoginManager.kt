package com.runerealms.auth.minecraft

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.runerealms.auth.RuneAuth
import com.runerealms.auth.encryption.EncryptionUtil
import com.runerealms.auth.minecraft.task.VerifyResponseTask
import com.runerealms.auth.util.disconnectPlayer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URI
import java.security.PublicKey
import java.util.UUID
import java.util.logging.Level

/**
 * This class will work as a proxy between the client and server.
 *
 * It will fake that the server runs in online mode, it will do everything that the server does to authenticate the player.
 * That includes generating keys, checking valid sessions, etc...
 *
 * Here we will only intercept **login** packets of **premium** players
 *
 * 1. Player connects
 * 2. We check if the username is premium
 * 3. We request a key from Mojang
 * 4. We check if all data is correct
 * 5. Then we encrypt the connection
 * 6. On sucess, intercept all login packets and fake a new login packet as a normal offline login
 */
class LoginManager(val authPlugin: RuneAuth): PacketAdapter(
    params()
        .plugin(authPlugin)
        .listenerPriority(ListenerPriority.HIGHEST)
        .types(PacketType.Login.Client.START, PacketType.Login.Client.ENCRYPTION_BEGIN)
        .optionAsync()
) {

    private val serverKeyPair = EncryptionUtil.generateKeypair()

    override fun onPacketReceiving(event: PacketEvent) {
        if (event.packetType == PacketType.Login.Client.START) {
            onLoginStart(event)
        } else if (event.packetType == PacketType.Login.Client.ENCRYPTION_BEGIN) {
            onEncryptionResponse(event)
        }
    }

    fun onLoginStart(event: PacketEvent) {
        authPlugin.loginSessions.remove(event.player.sessionId)
        val username = event.packet.strings.read(0)
        val mojangAuthResponse = checkIfUserIsPremium(username)
        if (mojangAuthResponse is MojangAuthResponse.RateLimited) {
            event.disconnectPlayer(
                Component.text("§c§lERRO TÉCNICO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("§fEsse erro não é culpa sua ou do servidor, não se preocupe!"))
                    .append(Component.text("§fAparentemente a §cMojang §festá com erros para confirmar sua conexão. Tente novamente em alguns segundos..."))
            )
            return
        } else if (mojangAuthResponse is MojangAuthResponse.InvalidUsername) {
            event.disconnectPlayer(
                Component.text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("§fSeu nome de usuário é inválido. Por favor, tente novamente."))
            )
            return
        } else if (mojangAuthResponse is MojangAuthResponse.NonPremium) {
            return
        }

        sendEncryptionRequest((mojangAuthResponse as MojangAuthResponse.Premium).properties, event)
    }

    @Suppress("Deprecation")
    fun onEncryptionResponse(event: PacketEvent) {
        val sharedSecret = event.packet.byteArrays.read(0)
        val session = authPlugin.loginSessions[event.player.sessionId]
        if (session == null) {
            event.disconnectPlayer(
                Component.text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("§fSua sessão expirou, por favor, tente novamente."))
            )
            return
        }

        val expectedVerifyToken = session.verifyToken
        val nonce = event.packet.byteArrays.read(1)
        val isNonceValid = EncryptionUtil.verifyNonce(expectedVerifyToken, serverKeyPair.private, nonce)
        if (!isNonceValid) {
            event.player.kickPlayer("Seu token de verificação não pode ser confirmado")
            return
        }

        event.asyncMarker.incrementProcessingDelay()
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin, VerifyResponseTask(
                plugin = authPlugin,
                player = event.player,
                sharedSecret = sharedSecret,
                packetEvent = event,
                session = session,
                serverKey = serverKeyPair
            )
        )
    }

    fun sendEncryptionRequest(properties: RawMojangAuthResponse, event: PacketEvent) {
        val verifyToken = EncryptionUtil.generateVerifyToken()

        val session = LoginSession(
            requestUsername = properties.username,
            requestedUuid = properties.uuid,
            verifyToken = verifyToken,
            publicKey = serverKeyPair.public,
            registered = false,
            nameAssociatedSkin = properties.textures.raw
        )
        authPlugin.loginSessions[event.player.sessionId] = session
        plugin.logger.info("Registering session for ${event.player.sessionId}")

        val encryptionBegin = PacketContainer(PacketType.Login.Server.ENCRYPTION_BEGIN)
        encryptionBegin.strings.write(0, "")

        val keyModifier = encryptionBegin.getSpecificModifier(PublicKey::class.java)
        val verifyField = if (keyModifier.fields.isEmpty()) {
            encryptionBegin.byteArrays.write(0, serverKeyPair.public.encoded)
            1
        } else {
            keyModifier.write(0, serverKeyPair.public)
            0
        }
        encryptionBegin.byteArrays.write(verifyField, verifyToken)

        authPlugin.protocolManager.sendServerPacket(event.player, encryptionBegin)
        synchronized(event.asyncMarker.processingLock) {
            event.isCancelled = true
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun checkIfUserIsPremium(username: String): MojangAuthResponse {
        if (username.length > 25) return MojangAuthResponse.InvalidUsername

        val url = URI("https://api.ashcon.app/mojang/v2/user/$username").toURL()
        val connection = try { url.openConnection() as HttpURLConnection } catch (exception: FileNotFoundException) {
            return MojangAuthResponse.NonPremium
        }
        var properties: RawMojangAuthResponse? = null
        runCatching {
            connection.connect()
        }.onSuccess {
            properties = Json.decodeFromStream<RawMojangAuthResponse>(connection.inputStream)
        }
        val responseCode = connection.responseCode
        return parseResponseCode(responseCode, properties)
    }

    private fun parseResponseCode(code: Int, properties: RawMojangAuthResponse?): MojangAuthResponse {
        return when (code) {
            200 -> MojangAuthResponse.Premium(properties ?: error("Properties are null"))
            429 -> MojangAuthResponse.RateLimited
            404, 204 -> MojangAuthResponse.NonPremium
            else -> {
                plugin.logger.log(Level.SEVERE, "Failed to check if user is premium for unknown code $code")
                MojangAuthResponse.NonPremium
            }
        }
    }

    @Serializable
    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    data class RawMojangAuthResponse(
        @SerialName("uuid")
        val _uuid: String,
        val username: String,
        val textures: Textures
    ) {
        val uuid: UUID get() = UUID.fromString(_uuid)

        @Serializable
        data class Textures(
            val custom: Boolean,
            val slim: Boolean,
            val raw: SkinProperty
        )
    }

    sealed interface MojangAuthResponse {
        data class Premium(val properties: RawMojangAuthResponse): MojangAuthResponse

        data object NonPremium : MojangAuthResponse

        data object RateLimited : MojangAuthResponse

        data object InvalidUsername : MojangAuthResponse
    }

    private companion object {
        val Json = Json {
            ignoreUnknownKeys = true
        }
    }
}
