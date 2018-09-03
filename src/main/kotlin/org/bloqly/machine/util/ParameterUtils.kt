package org.bloqly.machine.util

import com.google.common.primitives.UnsignedBytes
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.Validate
import org.bloqly.machine.lang.BInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
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

    private fun readLong(bis: ByteArrayInputStream): BInteger {

        val bytes = IOUtils.readFully(bis, EncodingUtils.LONG_BYTES)

        return BInteger(ByteBuffer.wrap(bytes).long)
    }

    private fun readBoolean(bis: ByteArrayInputStream): Boolean {

        return bis.read() == 1
    }

    fun writeParams(args: Array<Any>): ByteArray {

        Validate.isTrue(args.isNotEmpty())
        Validate.isTrue(args.size < 256)

        ByteArrayOutputStream().use { bos ->

            if (args.size > 1) {
                bos.write(0)
                bos.write(UnsignedBytes.checkedCast(args.size.toLong()).toInt())

                for (obj in args) {
                    bos.write(writeValue(obj))
                }
            } else {
                bos.write(writeValue(args[0]))
            }

            return bos.toByteArray()
        }
    }

    fun writeString(value: String): ByteArray {

        ByteArrayOutputStream().use {

            it.write(1)

            val bytes = value.toByteArray()

            val buffer = ByteBuffer.allocate(Integer.BYTES)
            buffer.putInt(bytes.size)
            it.write(buffer.array())

            it.write(bytes)

            return it.toByteArray()
        }
    }

    fun writeInteger(value: String): ByteArray {

        ByteArrayOutputStream().use {
            it.write(2)

            val buffer = ByteBuffer.allocate(Integer.BYTES)
            buffer.putInt(Integer.valueOf(value))
            it.write(buffer.array())

            return it.toByteArray()
        }
    }

    fun writeLong(value: String): ByteArray {

        ByteArrayOutputStream().use {
            it.write(3)

            it.write(EncodingUtils.longToBytes(value.toLong()))

            return it.toByteArray()
        }
    }

    fun writeLong(value: BigInteger): ByteArray {

        ByteArrayOutputStream().use {
            it.write(3)

            it.write(EncodingUtils.longToBytes(value.toLong()))

            return it.toByteArray()
        }
    }

    fun writeBoolean(value: String): ByteArray {

        ByteArrayOutputStream().use { bos ->
            bos.write(4)
            bos.write(if (java.lang.Boolean.valueOf(value)) 1 else 0)

            return bos.toByteArray()
        }
    }

    fun writeBoolean(value: Boolean): ByteArray {

        ByteArrayOutputStream().use { bos ->
            bos.write(4)
            bos.write(if (value) 1 else 0)

            return bos.toByteArray()
        }
    }

    private fun processString(value: String): ByteArray {
        val regex = """^BigInteger\((\d+)\)$""".toRegex()

        val result = regex.find(value)

        return if (result != null) {
            writeLong(result.groupValues.last())
        } else {
            writeString(value)
        }
    }

    fun writeValue(input: Any): ByteArray {

        return when (input) {
            is String ->
                processString(input.toString())
            is Int -> writeInteger(input.toString())
            is BInteger -> writeLong(input.value)
            is Boolean -> writeBoolean(input)
            else -> throw IllegalArgumentException(
                "Unsupported input $input of type ${input.javaClass.canonicalName}"
            )
        }
    }
}
