package org.bloqly.machine.util

import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import java.math.BigInteger

data class Signature(
    val e: ByteArray,
    val s: ByteArray
) {
    fun getE(): BigInteger = fromUnsignedByteArray(e)
    fun getS(): BigInteger = fromUnsignedByteArray(s)
}