package com.runerealms.auth.encryption

import com.google.common.annotations.Beta
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import java.math.BigInteger
import java.security.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random


object EncryptionUtil {
    fun generateKeypair(): KeyPair {
        val keypair = KeyPairGenerator.getInstance("RSA")
        keypair.initialize(2048)
        return keypair.genKeyPair()
    }

    fun generateVerifyToken(): ByteArray {
        val token = ByteArray(4)
        Random.nextBytes(token)
        return token
    }

    fun verifyNonce(
        expected: ByteArray,
        decryptionKey: PrivateKey,
        encryptedNonce: ByteArray
    ): Boolean {
        val nonce = decrypt(decryptionKey, encryptedNonce)
        return java.util.Arrays.equals(expected, nonce)
    }

    private fun decrypt(
        key: PrivateKey,
        data: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(key.algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun decryptSharedKey(privateKey: PrivateKey, sharedKey: ByteArray): SecretKey {
        return SecretKeySpec(decrypt(privateKey, sharedKey), "AES")
    }

    fun getServerIdHashString(serverId: String, sharedSecret: SecretKey, publicKey: PublicKey): String {
        val serverHash: ByteArray = getServerIdHash(serverId, publicKey, sharedSecret)
        return BigInteger(serverHash).toString(16)
    }

    private fun getServerIdHash(sessionId: String, publicKey: PublicKey, sharedSecret: SecretKey): ByteArray {
        val hasher: Hasher = Hashing.sha1().newHasher()

        hasher.putBytes(sessionId.toByteArray(Charsets.ISO_8859_1))
        hasher.putBytes(sharedSecret.encoded)
        hasher.putBytes(publicKey.encoded)

        return hasher.hash().asBytes()
    }
}