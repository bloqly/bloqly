package org.bloqly.machine.math

import org.bloqly.machine.exception.BloqlyArithmeticException
import java.math.BigInteger
import java.math.BigInteger.TEN
import java.util.Objects

class BInteger {

    val value: BigInteger

    @Suppress("unused")
    constructor(valueStr: String) {

        this.value = BigInteger(valueStr)

        assertValueInRange(this.value)
    }

    constructor(longValue: Long) {

        this.value = BigInteger.valueOf(longValue)

        assertValueInRange(this.value)
    }

    constructor(value: BigInteger) {

        this.value = value

        assertValueInRange(this.value)
    }

    @Suppress("unused")
    fun add(another: BInteger, max: BInteger): BInteger {

        val result = value.add(another.value)

        if (result > max.value) {
            throw BloqlyArithmeticException("The resulting value is too big $result > ${max.value}")
        }

        return BInteger(result)
    }

    @Suppress("unused")
    fun add(another: BInteger): BInteger {

        return BInteger(value.add(another.value))
    }

    @Suppress("unused")
    fun subtract(another: BInteger): BInteger {

        val result = value.subtract(another.value)

        if (result.signum() < 0) {
            throw BloqlyArithmeticException("Negative value: $result")
        }

        return BInteger(result)
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

    companion object {

        private val MAX_VALUE = TEN.pow(11).multiply(TEN.pow(8))

        private fun assertValueInRange(value: BigInteger) {

            require(value <= MAX_VALUE)
        }
    }
}
