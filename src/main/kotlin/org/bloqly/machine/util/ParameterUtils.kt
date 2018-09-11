package org.bloqly.machine.util

import com.google.common.primitives.UnsignedBytes
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.Validate
import org.bloqly.machine.lang.BLong
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.ArrayList

object ParameterUtils {

    fun readParams(arg: ByteArray): Array<Any> {

        if (ArrayUtils.isEmpty(arg)) {
            return arrayOf()
        }

        val result = ArrayList<Any>()

        val bis = ByteArrayInputStream(arg)

        val type = bis.read()

        if (type == 0) {
            val length = UnsignedBytes.toInt(bis.read().toByte())
            Validate.isTrue(length < 256)

            for (i in 0 until length) {
                result.add(readValue(bis))
            }
        } else {
            bis.reset()
            result.add(readValue(bis))
        }

        return result.toTypedArray()
    }

    fun readValue(bytes: ByteArray): Any =
        readValue(ByteArrayInputStream(bytes))

    private fun readValue(bis: ByteArrayInputStream): Any {

        val type = bis.read()

        return when (type) {
            1 -> readString(bis)
            2 -> readInteger(bis)
            3 -> readLong(bis)
            4 -> readBoolean(bis)
            else -> throw IllegalArgumentException("Unsupported type $type")
        }
    }

    private fun readString(bis: ByteArrayInputStream): String {

        val lengthBytes = IOUtils.readFully(bis, Integer.BYTES)
        val length = ByteBuffer.wrap(lengthBytes).int
        val bytes = IOUtils.readFully(bis, length)

        return String(bytes)
    }

    private fun readInteger(bis: ByteArrayInputStream): Int {

        val bytes = IOUtils.readFully(bis, Integer.BYTES)

        return ByteBuffer.wrap(bytes).int
    }

    private fun readLong(bis: ByteArrayInputStream): BLong {

        val bytes = IOUtils.readFully(bis, EncodingUtils.LONG_BYTES)

        return BLong(ByteBuffer.wrap(bytes).long)
    }

    private fun readBoolean(bis: ByteArrayInputStream): Boolean {

        return bis.read() == 1
    }

}
