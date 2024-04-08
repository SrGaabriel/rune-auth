package com.runerealms.auth.hash

import at.favre.lib.crypto.bcrypt.BCrypt

class HashingService(private val salt: String) {
    val hasher = BCrypt.withDefaults()
    val verifyer = BCrypt.verifyer()

    fun hashPassword(password: String): String =
        String(hasher.hash(COST, salt.toByteArray(CHARSET), password.toByteArray(CHARSET)), CHARSET)

    fun verifyPassword(password: String, hashed: String): BCrypt.Result =
        verifyer.verify(password.toByteArray(CHARSET), hashed.toByteArray(CHARSET))

    fun doesPasswordMatch(password: String, hashed: String): Boolean =
        verifyPassword(password, hashed).verified

    companion object {
        val COST = 11
        val CHARSET = Charsets.UTF_8
    }
}