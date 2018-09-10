package org.bloqly.machine.util

import org.bloqly.machine.lang.BLong
import org.junit.Assert.assertTrue
import org.junit.Test

class ParameterUtilsTest {

    @Test
    fun testReadWriteLong() {

        val value = ParameterUtils.writeValue("BigInteger(1)")

        val result = ParameterUtils.readValue(value)

        assertTrue(result is BLong)
    }
}