package org.bloqly.machine.component

import org.junit.Test

class CryptoServiceTest {

    private val cryptoService = CryptoService()

    @Test
    fun testCrypto() {

        val priv = cryptoService.generatePrivateKey()

        val pub = cryptoService.getPublicFor(priv)

        val signature = cryptoService.sign(priv, "test".toByteArray())

        println("signature.length = " + signature.size)

        val verified = cryptoService.verify("test".toByteArray(), signature, pub)

        println("verified = $verified")

    }

}
