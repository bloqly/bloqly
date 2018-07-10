package org.bloqly.machine.component

import org.bloqly.machine.util.CryptoUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun testCrypto() {

        val priv = CryptoUtils.generatePrivateKey()

        val pub = CryptoUtils.getPublicFor(priv)

        val signature = CryptoUtils.sign(priv, "test".toByteArray())

        println("signature.length = " + signature.size)

        val verified = CryptoUtils.verify("test".toByteArray(), signature, pub)

        assertTrue(verified)


    }
}
