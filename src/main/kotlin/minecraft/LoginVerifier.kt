package com.runerealms.auth.minecraft

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.util.UUID



object LoginVerifier {
    @OptIn(ExperimentalSerializationApi::class)
    fun hasJoined(username: String, serverHash: String, hostIp: InetAddress): Verification? {
        val url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=$username&serverId=$serverHash"

        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            return null
        }

        // Google query in site wiki vg
        // from:
        return Json.decodeFromStream<Verification>(connection.inputStream)
    }

    private val Json = Json {
        ignoreUnknownKeys = true
    }
}

@Serializable
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
data class Verification(
    @SerialName("id")
    val _id: String,
    val name: String?,
    val properties: List<SkinProperty>
) {
    val id: UUID get() = uuidFromString(_id)
}

private fun uuidFromString(hex: String): UUID {
    val mostSignificantBits = java.lang.Long.parseUnsignedLong(hex.substring(0, 16), 16)
    val leastSignificantBits = java.lang.Long.parseUnsignedLong(hex.substring(16), 16)
    return UUID(mostSignificantBits, leastSignificantBits)
}