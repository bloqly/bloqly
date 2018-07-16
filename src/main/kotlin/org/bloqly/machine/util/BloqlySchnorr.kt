package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import java.math.BigInteger
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

    fun newSecretKey(): BigInteger {

        return BigInteger(CURVE.n.bitLength(), secureRandom)
    }

    private fun jacobi(x: BigInteger): Boolean {
        // x(p-1)/2 mod p

        val p = (CURVE.curve as ECCurve.Fp).q

        val power = p.minus(BigInteger.ONE).divide(BigInteger.TWO)

        return x.modPow(power, p) == BigInteger.ONE
    }

    fun sign(message: ByteArray, d: BigInteger): Signature {

        var k = fromUnsignedByteArray(
            CryptoUtils.hash(
                Bytes.concat(
                    asUnsignedByteArray(d).pad32(),
                    message
                )
            )
        )

        val r = CURVE.g.multiply(k).normalize()

        if (jacobi(r.xCoord.toBigInteger())) {
            k = CURVE.n - k
        }

        val e = fromUnsignedByteArray(
            CryptoUtils.hash(
                Bytes.concat(
                    asUnsignedByteArray(r.xCoord.toBigInteger()).pad32(),
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

        return r.xCoord.toBigInteger() == signature.r
    }

    private fun ECPoint.encodePoint(): ByteArray = this.normalize().getEncoded(true)
}