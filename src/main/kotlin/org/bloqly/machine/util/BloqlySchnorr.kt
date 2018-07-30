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

    private const val CURVE_NAME = "secp256k1"

    private var secureRandom = SecureRandom.getInstance(CryptoUtils.RANDOM)

    private val CURVE_PARAMS = SECNamedCurves.getByName(CURVE_NAME)

    fun newBigInteger(hexString: String): BigInteger {
        return BigInteger(hexString, BASE_16)
    }

    fun newPrivateKey(): ByteArray {
        return asUnsignedByteArray(BigInteger(256, secureRandom)).pad()
    }

    private fun getP(): BigInteger {
        return (CURVE_PARAMS.curve as ECCurve.Fp).q
    }

    private fun jacobi(x: BigInteger): BigInteger {
        val p = getP()

        val power = p.minus(ONE).divide(TWO)

        return x.modPow(power, p)
    }

    fun sign(message: ByteArray, privateKey: ByteArray): Signature {
        return sign(message, fromUnsignedByteArray(privateKey))
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

        val r = CURVE_PARAMS.g.multiply(k).normalize()

        if (jacobi(r.yCoord.toBigInteger()) != ONE) {
            k = CURVE_PARAMS.n - k
        }

        val e = fromUnsignedByteArray(
            CryptoUtils.hash(
                Bytes.concat(
                    asUnsignedByteArray(r.xCoord.toBigInteger()).pad(),
                    CURVE_PARAMS.g.multiply(d).encodePoint(),
                    message
                )
            )
        )

        return Signature(
            r.xCoord.toBigInteger(),
            k.add(e.multiply(d)).mod(CURVE_PARAMS.n)
        )
    }

    fun getPublicFromPrivate(d: BigInteger): ByteArray {
        return CURVE_PARAMS.g.multiply(d).encodePoint()
    }

    fun verify(message: ByteArray, signature: Signature, p: ByteArray): Boolean {

        try {
            if (signature.r >= getP() || signature.s >= CURVE_PARAMS.n) {
                return false
            }

            // isValid is called inside
            val pub = CURVE_PARAMS.curve.decodePoint(p)

            val e = fromUnsignedByteArray(
                CryptoUtils.hash(
                    Bytes.concat(
                        signature.getR(),
                        pub.encodePoint(),
                        message
                    )
                )
            )

            val gS = CURVE_PARAMS.g.multiply(signature.s).normalize()

            val pubNE = pub.multiply(CURVE_PARAMS.n.minus(e)).normalize()

            val r = gS.add(pubNE).normalize()

            return r.xCoord.toBigInteger() == signature.r &&
                !r.isInfinity &&
                jacobi(r.yCoord.toBigInteger()) == ONE
        } catch (e: Exception) {
            return false
        }
    }

    private fun ECPoint.encodePoint(): ByteArray = this.normalize().getEncoded(true)
}