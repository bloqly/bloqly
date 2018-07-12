package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Vote
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

    @Test
    fun testVerifyVote() {

        val priv = CryptoUtils.generatePrivateKey()

        val pub = CryptoUtils.getPublicFor(priv)

        val vote = Vote(
            validatorId = "",
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
