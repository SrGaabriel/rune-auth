package com.runerealms.auth.minecraft

import org.bukkit.entity.Player
import java.security.PublicKey
import java.util.UUID

data class LoginSession(
    val requestUsername: String,
    val requestedUuid: UUID,
    val verifyToken: ByteArray,
    val publicKey: PublicKey,
    val registered: Boolean,
    val nameAssociatedSkin: SkinProperty?
) {
    var verifiedUsername: String? = null
    var uuid: UUID? = null
    var skinProperty: SkinProperty? = null
    var verified = false
}

internal val Player.sessionId: String get() = address.hostString + ":" + address.port
