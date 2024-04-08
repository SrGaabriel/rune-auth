package com.runerealms.auth.minecraft

import kotlinx.serialization.Serializable
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
data class SkinProperty(
    val value: String,
    val signature: String
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun isValid(publicKey: PublicKey?): Boolean {
        val sign: Signature
        try {
            sign = Signature.getInstance("SHA1withRSA")
        } catch (noSuckAlgEx: NoSuchAlgorithmException) {
            //SHA1withRSA should be present in all platforms
            throw AssertionError("The signature algorithm SHA1withRSA doesn't exist in this environment")
        }

        sign.initVerify(publicKey)
        sign.update(value.toByteArray())

        val decodedSignature: ByteArray = Base64.decode(signature)
        return sign.verify(decodedSignature)
    }
}