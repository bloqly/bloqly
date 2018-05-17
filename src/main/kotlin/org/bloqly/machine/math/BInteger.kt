package org.bloqly.machine.math

import org.apache.commons.lang3.Validate
import java.math.BigInteger
import java.math.BigInteger.TEN
import java.util.Objects

class BInteger {

    val value: BigInteger

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
    fun add(another: BInteger): BInteger {

        return BInteger(value.add(another.value))
    }

    @Suppress("unused")
    fun subtract(another: BInteger): BInteger {

        return BInteger(value.subtract(another.value))
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

            Validate.isTrue(value <= MAX_VALUE)
        }
    }
}
