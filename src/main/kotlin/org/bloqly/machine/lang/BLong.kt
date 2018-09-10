package org.bloqly.machine.lang

import java.math.BigInteger
import java.util.Objects

@Suppress("unused")
class BLong {

    private val maxValue = BigInteger.valueOf(Long.MAX_VALUE)

    private val minValue = BigInteger.valueOf(Long.MIN_VALUE)

    val value: BigInteger

    constructor(valueStr: String) {
        val result = BigInteger(valueStr.replace("_", ""))

        ensureRange(result)

        this.value = result
    }

    constructor(longValue: Long) {
        this.value = BigInteger.valueOf(longValue)
    }

    constructor(value: BigInteger) {
        ensureRange(value)
        this.value = value
    }

    fun pow(exponent: Int): BLong {

        require(exponent >= 0) {
            "Negative value: $exponent"
        }

        val result = value.pow(exponent)

        ensureRange(result)

        return BLong(result)
    }

    fun safeAdd(another: BLong, max: BLong): BLong {

        require(another.value.signum() >= 0) {
            "Negative value: ${another.value}"
        }

        val result = value.add(another.value)

        ensureRange(result)

        require(result <= max.value) {
            "The resulting value is too big $result > ${max.value}"
        }

        return BLong(result)
    }

    fun safeAdd(another: Long, max: BLong): BLong =
        safeAdd(BLong(another), max)

    fun add(another: BLong): BLong {
        val result = value.add(another.value)

        ensureRange(result)

        return BLong(result)
    }

    fun add(another: Long): BLong =
        add(BLong(another))

    fun safeSubtract(another: BLong): BLong {

        require(another.value.signum() >= 0) {
            "Negative value: ${another.value}"
        }

        val result = value.subtract(another.value)

        require(result.signum() >= 0) {
            "Negative value: $result"
        }

        ensureRange(result)

        return BLong(result)
    }

    fun safeSubtract(another: Long): BLong =
        safeSubtract(BLong(another))

    fun subtract(another: BLong): BLong {
        val result = value.subtract(another.value)

        ensureRange(result)

        return BLong(result)
    }

    fun subtract(another: Long): BLong =
        subtract(BLong(another))

    fun multiply(another: BLong): BLong {
        val result = value.multiply(another.value)

        ensureRange(result)

        return BLong(result)
    }

    fun safeMultiply(another: BLong, max: BLong): BLong {

        require(another.value.signum() >= 0) {
            "Negative value: ${another.value}"
        }

        val result = value.multiply(another.value)

        require(result <= max.value) {
            "The resulting value is too big $result > ${max.value}"
        }

        ensureRange(result)

        return BLong(value)
    }

    fun divide(another: BLong): BLong {
        val result = value.divide(another.value)

        return BLong(result)
    }

    private fun ensureRange(result: BigInteger) {
        require(result <= maxValue && result >= minValue) {
            "Result is out of range: $result"
        }
    }

    override fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val bLong = other as BLong?
        return value == bLong!!.value
    }

    override fun hashCode(): Int {
        return Objects.hash(value)
    }

    override fun toString(): String {
        return value.toString()
    }
}
