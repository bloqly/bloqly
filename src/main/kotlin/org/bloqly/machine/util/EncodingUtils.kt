package org.bloqly.machine.util

import com.google.common.io.BaseEncoding
import java.nio.ByteBuffer

object EncodingUtils {

    private val ENCODER16 = BaseEncoding.base16()

    const val LONG_BYTES = 8

    fun encodeToString(data: ByteArray?): String {
        data ?: throw IllegalArgumentException()
        return ENCODER16.encode(data)
    }

    fun decodeFromString(data: String?): ByteArray = ENCODER16.decode(data ?: throw RuntimeException())

    fun longToBytes(value: Long): ByteArray {

        return ByteBuffer.allocate(LONG_BYTES).putLong(value).array()
    }

}