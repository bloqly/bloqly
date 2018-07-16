package org.bloqly.machine.util

import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import java.math.BigInteger

data class Signature(
    val r: BigInteger,
    val s: BigInteger
) {
    fun getR(): ByteArray = asUnsignedByteArray(r)
    fun getS(): ByteArray = asUnsignedByteArray(s)
}