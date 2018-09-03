package org.bloqly.machine.component

import org.bitcoinj.core.ECKey
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.encode16
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
            val id = EncodingUtils.hashAndEncode16(publicKey)

            println(
                """
                id: $id
                publicKey: ${publicKey.encode16()}
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
            publicKey = pub.encode16(),
            blockHash = "",
            height = 11,
            timestamp = 1,
            spaceId = DEFAULT_SPACE
        )

        val signature = CryptoUtils.sign(
            priv,
            CryptoUtils.hash(vote)
        )

        val signedVote = vote.copy(signature = signature)

        assertTrue(CryptoUtils.verifyVote(signedVote, pub))
    }
}
