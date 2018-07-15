package org.bloqly.machine.util

import com.google.common.io.BaseEncoding
import java.math.BigInteger
import java.nio.ByteBuffer

fun String?.decode16(): ByteArray = BaseEncoding.base16().decode(this!!)
fun String?.decode64(): ByteArray = BaseEncoding.base64().decode(this!!)

fun ByteArray?.encode16(): String = BaseEncoding.base16().encode(this!!)
fun ByteArray?.encode64(): String = BaseEncoding.base64().encode(this!!)

object EncodingUtils {

    const val LONG_BYTES = 8
    const val INT_BYTES = 4

    fun longToBytes(value: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(value).array()

    fun intToBytes(value: Int): ByteArray = ByteBuffer.allocate(INT_BYTES).putInt(value).array()

    fun bigInt32ToBytes(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()

        val result = ByteArray(32)

        if (bytes[0].toInt() == 0) {
            System.arraycopy(bytes, 1, result, result.size - bytes.size + 1, bytes.size - 1)
        } else {
            System.arraycopy(bytes, 0, result, result.size - bytes.size, bytes.size)
        }

        return result
    }

    fun hashAndEncode16(input: ByteArray): String {
        val hash = CryptoUtils.hash(input)
        return hash.encode16()
    }
}