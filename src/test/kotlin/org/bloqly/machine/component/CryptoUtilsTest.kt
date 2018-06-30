package org.bloqly.machine.component

import junit.framework.Assert.assertTrue
import org.bloqly.machine.simulation.VoteType
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.vo.VoteVO
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun testCrypto() {

        val priv = CryptoUtils.generatePrivateKey()

        val pub = CryptoUtils.getPublicFor(priv)

        val signature = CryptoUtils.sign(priv, "test".toByteArray())

        println("signature.length = " + signature.size)

        val verified = CryptoUtils.verify("test".toByteArray(), signature, pub)

        println("verified = $verified")
    }

    @Test
    fun testValidateVote() {
        val voteVO = VoteVO(
            validatorId = "A3DDB47B4849BF1DD90580604405ABC91DECCFF68F6E787BDBEACED3F640B669",
            spaceId = "main",
            height = 8,
            voteType = VoteType.VOTE.name,
            blockId = "114FAAE4EA5F20AB1023BB295BA3CF6D4ED0D6FFD1864360B644C1FC1097731F",
            timestamp = 1530268725291,
            signature = "3045022100D270E150C1BC0C4CB1694A7520182744795A73A1F692ED09EAB0A7BC842D2B960220783651ACA1301BEB01EE85A651A9BAC4D41FDD948388068931D69C97E0B956CD",
            publicKey = "033E9D9E165DAC88157C4A17C74EB794D73DCA9315FA9223929FDA90F914EC7007"
        )

        assertTrue(CryptoUtils.verifyVote(voteVO.toModel()))
    }
}
