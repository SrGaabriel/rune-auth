package com.runerealms.auth.minecraft.task

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory
import com.comphenix.protocol.reflect.FuzzyReflection
import com.comphenix.protocol.reflect.accessors.Accessors
import com.comphenix.protocol.reflect.accessors.FieldAccessor
import com.comphenix.protocol.utility.MinecraftReflection
import com.comphenix.protocol.wrappers.Converters
import com.runerealms.auth.RuneAuth
import com.runerealms.auth.dao.Account
import com.runerealms.auth.encryption.EncryptionUtil
import com.runerealms.auth.minecraft.LoginSession
import com.runerealms.auth.minecraft.LoginVerifier
import com.runerealms.auth.minecraft.Verification
import com.runerealms.auth.util.disconnectPlayer
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.lang.reflect.Method
import java.net.InetSocketAddress
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyPair
import java.security.PublicKey
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey


class VerifyResponseTask(
    val plugin: RuneAuth,
    val packetEvent: PacketEvent,
    val player: Player,
    val session: LoginSession,
    val sharedSecret: ByteArray,
    val serverKey: KeyPair
): Runnable {
    private val premiumUuid = false

    override fun run() {
        try {
            verifyResponse(session)
        } finally {
            //this is a fake packet; it shouldn't be sent to the server
            synchronized(packetEvent.asyncMarker.processingLock) {
                packetEvent.isCancelled = true
            }

            ProtocolLibrary.getProtocolManager().asynchronousManager.signalPacketTransmission(packetEvent)
        }
    }

    private fun verifyResponse(session: LoginSession) {
        val privateKey = serverKey.private

        val loginKey: SecretKey
        try {
            loginKey = EncryptionUtil.decryptSharedKey(privateKey, sharedSecret)
        } catch (securityEx: GeneralSecurityException) {
            packetEvent.disconnectPlayer(
                Component
                    .text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Não conseguimos descriptografar as informações da conexão."))
            )
            return
        }

        try {
            if (!enableEncryption(loginKey)) {
                return
            }
        } catch (ex: Exception) {
            packetEvent.disconnectPlayer(
                Component
                    .text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Não conseguimos ativar a criptografia da conexão."))
            )
            return
        }

        val serverId: String = EncryptionUtil.getServerIdHashString("", loginKey, serverKey.public)

        val requestedUsername: String = session.requestUsername
        val socketAddress: InetSocketAddress = player.address
        try {
            val address = socketAddress.address
            plugin.logger.info("Server ID: $serverId Requested Username: $requestedUsername Address: $address")
            val response: Verification? = LoginVerifier.hasJoined(requestedUsername, serverId, address)
            if (response != null) {
                plugin.logger.info(response.properties.toString())
                encryptConnection(session, requestedUsername, response)
            } else {
                val realAccountUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:$requestedUsername").toByteArray(Charsets.UTF_8))
                val correspondingAccount = transaction {
                    Account.findById(realAccountUuid)
                }
                plugin.logger.info(correspondingAccount.toString())
                plugin.logger.info(correspondingAccount?.premium.toString())
                plugin.logger.info(correspondingAccount?.id?.value.toString())
                if (correspondingAccount == null || !correspondingAccount.premium) {
                    receiveFakeStartPacket(requestedUsername, serverKey.public, session.requestedUuid)
                    return
                }

                packetEvent.disconnectPlayer(
                    Component
                        .text("§c§lERRO")
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("Você ativou o modo de autenticação pelo Minecraft original, mas não está entrando com sua conta original."))
                )
            }
        } catch (ioEx: IOException) {
            packetEvent.disconnectPlayer(
                Component
                    .text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Houve um erro ao tentar autenticar você."))
            )
        }
    }

    private fun encryptConnection(session: LoginSession, requestedUsername: String, verification: Verification) {
        val realUsername = verification.name
        if (realUsername == null) {
            packetEvent.disconnectPlayer(
                Component
                    .text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("A Mojang não nos enviou informações suficientes para te autenticar."))
            )
            return
        }
        plugin.logger.info("Player ${session.requestUsername} (${requestedUsername}) successfully authenticated as $realUsername")

        val properties = verification.properties
        if (properties.isNotEmpty()) {
            session.skinProperty = properties[0]
        }

        session.verifiedUsername = realUsername
        session.uuid = verification.id
        session.verified = true

//        setPremiumUUID(session.uuid)
        plugin.logger.info("Player $realUsername is a premium account")
        plugin.logger.info(plugin.authenticationStates.toString())
        receiveFakeStartPacket(realUsername, session.publicKey, session.uuid!!)
    }

    private fun setPremiumUUID(premiumUUID: UUID?) {
        if (premiumUUID != null) {
            try {
                val networkManager = getNetworkManager()

                //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/NetworkManager.java#L69
                val managerClass: Class<*> = networkManager.javaClass
                val accessor: FieldAccessor =
                    Accessors.getFieldAccessorOrNull(managerClass, "spoofedUUID", UUID::class.java)
                accessor.set(networkManager, premiumUUID)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }
    }

    //try to get the networkManager from ProtocolLib
    @Throws(ClassNotFoundException::class)
    private fun getNetworkManager(): Any {
        val injectorContainer: Any = TemporaryPlayerFactory.getInjectorFromPlayer(player)

        // ChannelInjector
        val injectorClass = Class.forName("com.comphenix.protocol.injector.netty.Injector")
        val rawInjector = FuzzyReflection.getFieldValue(injectorContainer, injectorClass, true)

        val rawInjectorClass: Class<*> = rawInjector.javaClass
        val accessor: FieldAccessor =
            Accessors.getFieldAccessorOrNull(rawInjectorClass, "networkManager", Any::class.java)
        return accessor.get(rawInjector)
    }

    @Throws(IllegalArgumentException::class)
    private fun enableEncryption(loginKey: SecretKey): Boolean {
        if (encryptMethod == null) {
            val networkManagerClass = MinecraftReflection.getNetworkManagerClass()

            try {
                // Try to get the old (pre MC 1.16.4) encryption method
                encryptMethod = FuzzyReflection.fromClass(networkManagerClass)
                    .getMethodByParameters("a", SecretKey::class.java)
            } catch (exception: IllegalArgumentException) {
                // Get the new encryption method
                encryptMethod = FuzzyReflection.fromClass(networkManagerClass)
                    .getMethodByParameters("a", Cipher::class.java, Cipher::class.java)

                // Get the needed Cipher helper method (used to generate ciphers from login key)
                cipherMethod = FuzzyReflection.fromClass(ENCRYPTION_CLASS)
                    .getMethodByParameters("a", Int::class.javaPrimitiveType, Key::class.java)
            }
        }

        try {
            val networkManager = this.getNetworkManager()

            // If cipherMethod is null - use old encryption (pre MC 1.16.4), otherwise use the new cipher one
            if (cipherMethod == null) {
                // Encrypt/decrypt packet flow, this behaviour is expected by the client
                encryptMethod?.invoke(networkManager, loginKey)
            } else {
                // Create ciphers from login key
                val decryptionCipher: Any = cipherMethod!!.invoke(null, Cipher.DECRYPT_MODE, loginKey)
                val encryptionCipher: Any = cipherMethod!!.invoke(null, Cipher.ENCRYPT_MODE, loginKey)

                // Encrypt/decrypt packet flow, this behaviour is expected by the client
                encryptMethod?.invoke(networkManager, decryptionCipher, encryptionCipher)
            }
        } catch (ex: Exception) {
            packetEvent.disconnectPlayer(
                Component
                    .text("§c§lERRO")
                    .append(Component.newline())
                    .append(Component.text("Não conseguimos ativar a criptografia da conexão."))
            )
            return false
        }

        return true
    }

    //fake a new login packet in order to let the server handle all the other stuff
    private fun receiveFakeStartPacket(username: String, clientKey: PublicKey, uuid: UUID) {
        val startPacket = PacketContainer(PacketType.Login.Client.START)
        startPacket.strings.write(0, username)
        startPacket.getOptionals(Converters.passthrough(UUID::class.java)).write(0, Optional.of(uuid))

        ProtocolLibrary.getProtocolManager().receiveClientPacket(player, startPacket, false)
    }

    companion object {
        private var encryptMethod: Method? = null
        private var cipherMethod: Method? = null

        private const val ENCRYPTION_CLASS_NAME = "MinecraftEncryption"
        private var ENCRYPTION_CLASS: Class<*>? = null

        init {
            ENCRYPTION_CLASS = MinecraftReflection.getMinecraftClass(
                "util.$ENCRYPTION_CLASS_NAME", ENCRYPTION_CLASS_NAME
            )
        }
    }
}