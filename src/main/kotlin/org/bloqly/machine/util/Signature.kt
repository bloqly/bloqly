package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import java.math.BigInteger

data class Signature(
    val r: BigInteger,
    val s: BigInteger
) {
    fun getR(): ByteArray = asUnsignedByteArray(32, r)
    fun getS(): ByteArray = asUnsignedByteArray(32, s)

    override fun toString(): String {
        val bytes = Bytes.concat(
            getR(),
            getS()
        )

        return bytes.encode16()
    }

    companion object {
        fun fromString(sigStr: String): Signature {
            val sigBytes = sigStr.decode16()

            val r = fromUnsignedByteArray(sigBytes.slice(0..31).toByteArray())
            val s = fromUnsignedByteArray(sigBytes.slice(32..63).toByteArray())

            return Signature(r, s)
        }
    }
}