package org.bloqly.machine.util

import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class BloqlySchnorrTest {

    @Test
    fun testSignAndVerify() {

        val d = BloqlySchnorr.newSecretKey()

        val p = BloqlySchnorr.getPublicFromPrivate(d)

        val message = "hi".toByteArray()

        val signature = BloqlySchnorr.sign(message, d)

        assertTrue(BloqlySchnorr.verify(message, signature, p))
    }

    @Test
    fun testVector1() {
        val message = asUnsignedByteArray(BigInteger("0000000000000000000000000000000000000000000000000000000000000000", 16)).pad32()

        val d = BigInteger("0000000000000000000000000000000000000000000000000000000000000001", 16)

        val p = asUnsignedByteArray(BigInteger("0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)).pad32()

        val signature = BloqlySchnorr.sign(message, d)

        assertTrue(BloqlySchnorr.verify(message, signature, p))
    }

    @Test
    fun testEncoding() {

        val d = BloqlySchnorr.newSecretKey()

        assertEquals(d, BigIntegers.fromUnsignedByteArray(asUnsignedByteArray(d)))
    }
}