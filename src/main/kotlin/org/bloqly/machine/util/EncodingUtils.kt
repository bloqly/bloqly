package org.bloqly.machine.util

import org.apache.commons.codec.binary.Hex
import java.nio.ByteBuffer

fun String?.fromHex(): ByteArray = Hex.decodeHex(this)

fun ByteArray?.toHex(): String = String(Hex.encodeHex(this!!)).toUpperCase()

object EncodingUtils {

    const val LONG_BYTES = 8
    private const val INT_BYTES = 4

    fun longToBytes(value: Long): ByteArray = ByteBuffer.allocate(LONG_BYTES).putLong(value).array()

    fun intToBytes(value: Int): ByteArray = ByteBuffer.allocate(INT_BYTES).putInt(value).array()
}