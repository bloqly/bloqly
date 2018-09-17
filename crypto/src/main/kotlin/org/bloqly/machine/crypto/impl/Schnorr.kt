package org.bloqly.machine.crypto.impl

import com.google.common.primitives.Bytes
import org.bloqly.machine.crypto.CryptoUtils
import org.bloqly.machine.crypto.CryptoUtils.CURVE_PARAMS
import org.bloqly.machine.crypto.Signature
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import java.math.BigInteger
import java.math.BigInteger.ONE
import java.math.BigInteger.TWO

object Schnorr {

    private fun getP(): BigInteger {
        return (CURVE_PARAMS.curve as ECCurve.Fp).q
    }

    private fun jacobi(x: BigInteger): BigInteger {
        val p = getP()

        val power = p.minus(ONE).divide(TWO)

        return x.modPow(power, p)
    }

    fun sign(message: ByteArray, privateKey: ByteArray): Signature {
        require(32 == privateKey.size)
        return sign(message, fromUnsignedByteArray(privateKey))
    }

    fun sign(message: ByteArray, d: BigInteger): Signature {

        require(32 == message.size)

        var k = fromUnsignedByteArray(
            CryptoUtils.hash(
                Bytes.concat(
                    asUnsignedByteArray(32, d),
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
                    asUnsignedByteArray(32, r.xCoord.toBigInteger()),
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

    fun verify(message: ByteArray, signature: Signature, p: ByteArray): Boolean {

        require(64 == signature.toByteArray().size)
        require(33 == p.size)

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