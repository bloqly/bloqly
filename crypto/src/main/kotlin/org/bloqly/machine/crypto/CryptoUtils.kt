package org.bloqly.machine.crypto

import org.bitcoinj.core.ECKey
import org.bloqly.machine.crypto.impl.Schnorr
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun String?.fromHex(): ByteArray = Hex.decode(this)

fun ByteArray?.toHex(): String = String(Hex.encode(this!!)).toUpperCase()

fun String.toHexBigInteger() = BigInteger(1, Hex.decode(this))

fun ByteArray.toAddress(): String = CryptoUtils.hash(this).toHex()

fun String.toAddress(): String = CryptoUtils.hash(this).toHex()

object CryptoUtils {

    private const val SHA_256 = "SHA-256"
    private const val AES = "AES"
    private const val AES_PADDING = "AES/CBC/PKCS5Padding"
    private const val AES_IV_SIZE = 16
    private const val AES_INPUT_SIZE = 32

    internal const val CURVE_NAME = "secp256k1"
    internal val CURVE_PARAMS = SECNamedCurves.getByName(CURVE_NAME)

    fun encrypt(input: ByteArray?, passphrase: String): ByteArray {
        require(input != null)
        require(input!!.size == AES_INPUT_SIZE)
        require(passphrase.isNotEmpty())

        val ivSize = AES_IV_SIZE
        val iv = ByteArray(ivSize)
        val random = SecureRandom()
        random.nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)

        val key = SecretKeySpec(
            hash(passphrase),
            AES
        )
        val cipher = Cipher.getInstance(AES_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec)

        val encrypted = cipher.doFinal(input)

        val encryptedIVAndText = ByteArray(ivSize + encrypted.size)
        System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize)
        System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.size)

        return encryptedIVAndText
    }

    fun decrypt(input: ByteArray?, passphrase: String): ByteArray {
        require(input != null)

        val iv = ByteArray(AES_IV_SIZE)
        System.arraycopy(input, 0, iv, 0, iv.size)
        val ivParameterSpec = IvParameterSpec(iv)

        val encryptedSize = input!!.size - AES_IV_SIZE
        val encryptedBytes = ByteArray(encryptedSize)
        System.arraycopy(input, AES_IV_SIZE, encryptedBytes, 0, encryptedSize)

        val key = SecretKeySpec(
            hash(passphrase),
            AES
        )
        val cipher = Cipher.getInstance(AES_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec)
        return cipher.doFinal(encryptedBytes)
    }

    fun getPublicFor(privateKeyBytes: ByteArray?): ByteArray {

        val privateKey = BigIntegers.fromUnsignedByteArray(privateKeyBytes)

        return ECKey.publicKeyFromPrivate(privateKey, true)
    }

    fun hash(input: ByteArray): ByteArray {
        return MessageDigest.getInstance(SHA_256).digest(input)
    }

    fun hash(input: String): ByteArray {
        return hash(input.fromHex())
    }

    fun sign(privateKey: ByteArray?, input: ByteArray): ByteArray =
        Schnorr.sign(input, privateKey!!).toByteArray()

    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        if (signature.isEmpty()) {
            return false
        }
        return Schnorr.verify(message, Signature.fromByteArray(signature), publicKey)
    }

    fun verifySchnorrHex(message: String, signature: String, publicKey: String): Boolean =
        verify(
            Hex.decode(message),
            Hex.decode(signature),
            Hex.decode(publicKey)
        )

    fun verifyHex(message: String, signature: String, publicKey: String): Boolean =
        ECKey.verify(
            Hex.decode(message),
            Hex.decode(signature),
            Hex.decode(publicKey)
        )
}
