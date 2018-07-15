package org.bloqly.machine.util

import org.bouncycastle.util.encoders.Hex
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class BloqlySchnorrTest {

    @Test
    fun testVector1() {

        val d = BloqlySchnorr.newBigInteger("0000000000000000000000000000000000000000000000000000000000000001")
        val P = BloqlySchnorr.newBigInteger("0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798")

        val message = ByteArray(32)
        val data = BloqlySchnorr.newBigInteger("0000000000000000000000000000000000000000000000000000000000000000")
            .toByteArray()
        System.arraycopy(data, 0, message, 0, data.size)

        val expectedSignature =
            "787A848E71043D280C50470E8E1532B2DD5D20EE912A45DBDD2BD1DFBF187EF67031A98831859DC34DFFEEDDA86831842CCD0079E1F92AF177F7F22CC1DCED05"

        val signature = BloqlySchnorr.sign(message, d)

        val sig = BigInteger(signature).toString(16).toUpperCase()

        assertEquals(expectedSignature, sig)
    }

    @Test
    fun testToBytes() {

        val P = BloqlySchnorr.newBigInteger("C90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B14E5C7")

        val bytes = EncodingUtils.bigInt32ToBytes(P)

        println(Hex.toHexString(bytes))
    }
}