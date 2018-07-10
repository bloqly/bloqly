package org.bloqly.machine.util

import com.google.common.io.BaseEncoding
import java.nio.ByteBuffer

fun String?.decode16(): ByteArray = BaseEncoding.base16().decode(this!!)
fun String?.decode64(): ByteArray = BaseEncoding.base64().decode(this!!)

fun ByteArray?.encode16(): String = BaseEncoding.base16().encode(this!!)
fun ByteArray?.encode64(): String = BaseEncoding.base64().encode(this!!)

object EncodingUtils {

    const val LONG_BYTES = 8

    fun longToBytes(value: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(value).array()

    fun intToBytes(value: Int): ByteArray = ByteBuffer.allocate(LONG_BYTES).putInt(value).array()

    fun hashAndEncode16(input: ByteArray): String {
        val hash = CryptoUtils.digest(input)
        return hash.encode16()
    }
}