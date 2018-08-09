package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Account
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
            val privateKey = CryptoUtils.newPrivateKey()
            val publicKey = CryptoUtils.getPublicFor(privateKey)
            val id = EncodingUtils.hashAndEncode16(publicKey)

            println(
                """
                id: $id
                publicKey: ${publicKey.encode16()}
                privateKey: ${privateKey.encode16()}
            """.trimIndent()
            )
        }
    }

    @Test
    fun testCrypto() {

        val priv = CryptoUtils.newPrivateKey()

        val pub = CryptoUtils.getPublicFor(priv)

        val message = CryptoUtils.hash("test".toByteArray())

        val signature = CryptoUtils.sign(priv, message)

        println("signature.length = " + signature.size)

        val verified = CryptoUtils.verify(message, signature, pub)

        assertTrue(verified)
    }

    @Test
    fun testVerifyVote() {

        val priv = CryptoUtils.newPrivateKey()

        val pub = CryptoUtils.getPublicFor(priv)

        val vote = Vote(
            validator = Account(
                accountId = EncodingUtils.hashAndEncode16(pub),
                publicKey = pub.encode16()
            ),
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
