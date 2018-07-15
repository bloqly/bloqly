package org.bloqly.machine.util

import org.junit.Assert.assertTrue
import org.junit.Test

class BloqlySchnorrTest {

    @Test
    fun testSignAndVerify() {

        val d = BloqlySchnorr.newSecretKey()

        val p = BloqlySchnorr.getPublicFormPrivate(d)

        val message = "hi".toByteArray()

        val signature = BloqlySchnorr.sign(message, d)

        assertTrue(BloqlySchnorr.verify(message, signature, p))
    }
}