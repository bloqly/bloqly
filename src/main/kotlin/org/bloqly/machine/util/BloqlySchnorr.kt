package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.TWO
import java.security.SecureRandom

object BloqlySchnorr {
    private const val BASE_16 = 16

    private const val RANDOM = "SHA1PRNG"

    private const val CURVE_NAME = "secp256k1"

    private var secureRandom = SecureRandom.getInstance(RANDOM)

    private val CURVE = SECNamedCurves.getByName(CURVE_NAME)

    fun newBigInteger(hexString: String): BigInteger {
        return BigInteger(hexString, BASE_16)
    }

    fun newSecretKey(): ByteArray {
        return asUnsignedByteArray(BigInteger(256, secureRandom))
    }

    private fun getP(): BigInteger {
        return (CURVE.curve as ECCurve.Fp).q
    }

    private fun jacobi(x: BigInteger): BigInteger {
        val p = getP()

        val power = p.minus(ONE).divide(TWO)

        return x.modPow(power, p)
    }

    fun sign(message: ByteArray, d: BigInteger): Signature {

        var k = fromUnsignedByteArray(
            CryptoUtils.hash(
                Bytes.concat(
                    asUnsignedByteArray(d).pad(),
                    message
                )
            )
        )

        val r = CURVE.g.multiply(k).normalize()

        if (jacobi(r.yCoord.toBigInteger()) != ONE) {
            k = CURVE.n - k
        }

        val e = fromUnsignedByteArray(
            CryptoUtils.hash(
                Bytes.concat(
                    asUnsignedByteArray(r.xCoord.toBigInteger()).pad(),
                    CURVE.g.multiply(d).encodePoint(),
                    message
                )
            )
        )

        return Signature(
            r.xCoord.toBigInteger(),
            k.add(e.multiply(d)).mod(CURVE.n)
        )
    }

    fun getPublicFromPrivate(d: BigInteger): ByteArray {
        return CURVE.g.multiply(d).encodePoint()
    }

    fun verify(message: ByteArray, signature: Signature, p: ByteArray): Boolean {

        try {
            if (signature.r >= getP() || signature.s >= CURVE.n) {
                return false
            }

            // isValid is called inside
            val pub = CURVE.curve.decodePoint(p)

            val e = fromUnsignedByteArray(
                CryptoUtils.hash(
                    Bytes.concat(
                        signature.getR(),
                        pub.encodePoint(),
                        message
                    )
                )
            )

            val gS = CURVE.g.multiply(signature.s).normalize()

            val pubNE = pub.multiply(CURVE.n.minus(e)).normalize()

            val r = gS.add(pubNE).normalize()

            //if r >= p or s >= n:

            return r.xCoord.toBigInteger() == signature.r &&
                !r.isInfinity &&
                jacobi(r.yCoord.toBigInteger()) == ONE
        } catch (e: Exception) {
            return false
        }
    }

    private fun ECPoint.encodePoint(): ByteArray = this.normalize().getEncoded(true)
}