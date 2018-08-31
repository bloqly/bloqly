package org.bloqly.machine.math

import java.math.BigInteger
import java.util.Objects

@Suppress("unused")
class BInteger {

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

    fun pow(exponent: Int): BInteger {

        require(exponent >= 0) {
            "Negative value: $exponent"
        }

        val result = value.pow(exponent)

        ensureRange(result)

        return BInteger(result)
    }

    fun safeAdd(another: BInteger, max: BInteger): BInteger {

        require(another.value.signum() >= 0) {
            "Negative value: ${another.value}"
        }

        val result = value.add(another.value)

        ensureRange(result)

        require(result <= max.value) {
            "The resulting value is too big $result > ${max.value}"
        }

        return BInteger(result)
    }

    fun safeAdd(another: Long, max: BInteger): BInteger =
        safeAdd(BInteger(another), max)

    fun add(another: BInteger): BInteger {
        val result = value.add(another.value)

        ensureRange(result)

        return BInteger(result)
    }

    fun add(another: Long): BInteger =
        add(BInteger(another))

    fun safeSubtract(another: BInteger): BInteger {

        require(another.value.signum() >= 0) {
            "Negative value: ${another.value}"
        }

        val result = value.subtract(another.value)

        require(result.signum() >= 0) {
            "Negative value: $result"
        }

        ensureRange(result)

        return BInteger(result)
    }

    fun safeSubtract(another: Long): BInteger =
        safeSubtract(BInteger(another))

    fun subtract(another: BInteger): BInteger {
        val result = value.subtract(another.value)

        ensureRange(result)

        return BInteger(result)
    }

    fun subtract(another: Long): BInteger =
        subtract(BInteger(another))

    fun multiply(another: BInteger): BInteger {
        val result = value.multiply(another.value)

        ensureRange(result)

        return BInteger(value)
    }

    fun safeMultiply(another: BInteger, max: BInteger): BInteger {

        require(another.value.signum() >= 0) {
            "Negative value: ${another.value}"
        }

        val result = value.multiply(another.value)

        require(result <= max.value) {
            "The resulting value is too big $result > ${max.value}"
        }

        ensureRange(result)

        return BInteger(value)
    }

    fun divide(another: BInteger): BInteger {
        val result = value.divide(another.value)

        return BInteger(result)
    }

    private fun ensureRange(result: BigInteger) {
        require(result <= maxValue && result >= minValue) {
            "Result is out of range: $result"
        }
    }

    override fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val bInteger = other as BInteger?
        return value == bInteger!!.value
    }

    override fun hashCode(): Int {
        return Objects.hash(value)
    }

    override fun toString(): String {
        return value.toString()
    }
}
