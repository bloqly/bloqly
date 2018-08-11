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
    private fun getS(): ByteArray = asUnsignedByteArray(32, s)

    override fun toString(): String = toByteArray().encode16()

    fun toByteArray(): ByteArray = Bytes.concat(getR(), getS())

    companion object {
        fun fromString(sigStr: String): Signature = fromByteArray(sigStr.decode16())

        fun fromByteArray(sigBytes: ByteArray): Signature {

            val r = fromUnsignedByteArray(sigBytes.slice(0..31).toByteArray())
            val s = fromUnsignedByteArray(sigBytes.slice(32..63).toByteArray())

            return Signature(r, s)
        }
    }
}