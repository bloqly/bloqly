package org.bloqly.machine.util

import org.bouncycastle.util.encoders.Hex
import java.math.BigInteger
import java.nio.ByteBuffer

fun String?.decode16(): ByteArray = Hex.decode(this)

fun ByteArray?.encode16(): String = String(Hex.encode(this!!)).toUpperCase()

fun String.toHexBigInteger() = BigInteger(1, Hex.decode(this))

fun ByteArray.pad(): ByteArray {

    val size = if (this.size > 32) this.size else 32

    val result = ByteArray(size)

    System.arraycopy(this, 0, result, result.size - this.size, this.size)

    return result
}

object EncodingUtils {

    const val LONG_BYTES = 8
    private const val INT_BYTES = 4

    fun longToBytes(value: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(value).array()

    fun intToBytes(value: Int): ByteArray = ByteBuffer.allocate(INT_BYTES).putInt(value).array()

    fun hashAndEncode16(input: ByteArray): String {
        val hash = CryptoUtils.hash(input)
        return hash.encode16()
    }
}