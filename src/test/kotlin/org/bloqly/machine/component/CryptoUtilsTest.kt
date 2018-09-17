package org.bloqly.machine.component

import org.bitcoinj.core.ECKey
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.crypto.CryptoUtils
import org.bloqly.machine.crypto.toAddress
import org.bloqly.machine.helper.CryptoHelper
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.toHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun testEncrypt() {
        val secret = CryptoUtils.hash("a secret")
        val password = "a password"

        val encrypted = CryptoUtils.encrypt(secret, password)

        val decryptedSecret = CryptoUtils.decrypt(encrypted, password)

        assertArrayEquals(secret, decryptedSecret)
    }

    @Test
    fun testNewAccount() {

        for (i in 0..5) {

            val key = ECKey()

            val privateKey = key.privateKeyAsHex
            val publicKey = key.pubKey
            val id = publicKey.toAddress()

            println(
                """
                id: $id
                publicKey: ${publicKey.toHex()}
                privateKey: $privateKey
            """.trimIndent()
            )
        }
    }

    @Test
    fun testCrypto() {

        val key = ECKey()

        val priv = key.privKeyBytes

        val pub = key.pubKey

        val message = CryptoUtils.hash("test".toByteArray())

        val signature = CryptoUtils.sign(priv, message)

        println("signature.length = " + signature.size)

        val verified = CryptoUtils.verify(message, signature, pub)

        assertTrue(verified)
    }

    @Test
    fun testVerifyVote() {

        val key = ECKey()

        val priv = key.privKeyBytes
        val pub = key.pubKey

        val vote = Vote(
            publicKey = pub.toHex(),
            blockHash = "",
            height = 11,
            timestamp = 1,
            spaceId = DEFAULT_SPACE
        )

        val signature = CryptoUtils.sign(
            priv,
            CryptoHelper.hash(vote)
        )

        val signedVote = vote.copy(signature = signature)

        assertTrue(CryptoHelper.verifyVote(signedVote, pub))
    }
}
