package org.bloqly.machine.util

import com.google.common.io.BaseEncoding
import java.nio.ByteBuffer

object EncodingUtils {

    private val ENCODER16 = BaseEncoding.base16()
    private val ENCODER64 = BaseEncoding.base64()

    const val LONG_BYTES = 8

    fun encodeToString64(data: ByteArray?): String {
        data ?: throw IllegalArgumentException()
        return ENCODER64.encode(data)
    }

    fun encodeToString16(data: ByteArray?): String {
        data ?: throw IllegalArgumentException()
        return ENCODER16.encode(data)
    }

    fun decodeFromString16(data: String?): ByteArray = ENCODER16.decode(data ?: throw RuntimeException())

    fun decodeFromString64(data: String?): ByteArray = ENCODER64.decode(data ?: throw RuntimeException())

    fun longToBytes(value: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(value).array()

    fun intToBytes(value: Int): ByteArray = ByteBuffer.allocate(LONG_BYTES).putInt(value).array()

    fun hashAndEncode16(input: ByteArray): String {
        val hash = CryptoUtils.digest(input)
        return EncodingUtils.encodeToString16(hash)
    }
}