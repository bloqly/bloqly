package org.bloqly.machine.util

import org.bouncycastle.asn1.sec.SECNamedCurves
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

    fun sign(message: ByteArray, d: BigInteger): ByteArray {

        /*
        k = sha256(seckey.to_bytes(32, byteorder="big") + msg)
        R = point_mul(G, k)
        if jacobi(R[1]) != 1:
        k = n - k
        e = sha256(R[0].to_bytes(32, byteorder="big") + bytes_point(point_mul(G, seckey)) + msg)
        return R[0].to_bytes(32, byteorder="big") + ((k + e * seckey) % n).to_bytes(32, byteorder="big")
         */

        val bytes = hash(concat(EncodingUtils.bigInt32ToBytes(d), message))

        //BigInteger k = new BigInteger(bytes).mod(CURVE.getN());

        val k = BigInteger(1, bytes)

        val R = CURVE.g.multiply(k).normalize()

        // jacobi?
        // k = CURVE.getN().subtract(k);

        val e = BigInteger(
            hash(
                concat(
                    R.xCoord.toBigInteger().toByteArray(),
                    CURVE.g.multiply(d).getEncoded(false),
                    message
                )
            )
        ).mod(CURVE.n)

        // bytes(x(R)) || bytes(k + ex mod n).

        return concat(
            R.xCoord.toBigInteger().toByteArray(),
            k.add(e.multiply(d)).mod(CURVE.n).toByteArray()
        )
    }
}