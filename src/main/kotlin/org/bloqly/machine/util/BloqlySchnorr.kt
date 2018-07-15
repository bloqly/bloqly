package org.bloqly.machine.util

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

object BloqlySchnorr {
    private const val BASE_16 = 16

    private const val SHA_256 = "SHA-256"

    private const val RANDOM = "SHA1PRNG"

    private const val CURVE_NAME = "secp256k1"

    private var secureRandom = SecureRandom.getInstance(RANDOM)

    private val CURVE = SECNamedCurves.getByName(CURVE_NAME)

    fun newBigInteger(hexString: String): BigInteger {
        return BigInteger(hexString, BASE_16)
    }

    private fun hash(input: ByteArray): ByteArray {

        try {
            return MessageDigest.getInstance(SHA_256).digest(input)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    private fun concat(left: ByteArray, right: ByteArray): ByteArray {

        val result = ByteArray(left.size + right.size)

        System.arraycopy(left, 0, result, 0, left.size)
        System.arraycopy(right, 0, result, left.size, right.size)

        return result
    }

    private fun concat(left: ByteArray, middle: ByteArray, right: ByteArray): ByteArray {

        return concat(concat(left, middle), right)
    }

    fun newSecretKey(): BigInteger {

        return BigInteger(CURVE.n.bitLength(), secureRandom)
    }

    fun sign(message: ByteArray, d: BigInteger): Signature {
        // compute k

        val k = fromUnsignedByteArray(
            hash(
                concat(
                    message,
                    asUnsignedByteArray(d)
                )
            )
        )

        // e = H(m || k * G)

        val kG = CURVE.g.multiply(k).normalize().getEncoded(false)

        val e = fromUnsignedByteArray(hash(concat(message, kG)))

        // s = k – e * x

        val s = k.minus(e.multiply(d))

        return Signature(
            asUnsignedByteArray(e),
            asUnsignedByteArray(s)
        )
    }

    fun getPublicFormPrivate(d: BigInteger): BigInteger {
        return fromUnsignedByteArray(CURVE.g.multiply(d).normalize().getEncoded(false))
    }

    fun verify(message: ByteArray, signature: Signature, p: BigInteger): Boolean {
        // H(m || s * G + e * P)

        val sG = fromUnsignedByteArray(CURVE.g.multiply(signature.getS()).normalize().getEncoded(false))

        val eP = p.multiply(signature.getE())

        val hash = hash(
            concat(
                message,
                asUnsignedByteArray(sG.add(eP))
            )
        )

        return hash.contentEquals(signature.e)
    }
}