package org.bloqly.machine.util

import org.bitcoinj.core.ECKey
import org.bouncycastle.util.BigIntegers.fromUnsignedByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloqlySchnorrTest {

    @Test
    fun testCompatibility() {

        val keyPair = ECKey()

        val ecPrivateKey = keyPair.privateKeyAsHex!!

        val ecPublicKey = keyPair.publicKeyAsHex!!.toUpperCase()

        val publicKeyBytes = CryptoUtils.getPublicFor(ecPrivateKey.decode16())

        val publicKey = publicKeyBytes.encode16()

        assertEquals(ecPublicKey, publicKey)
    }

    @Test
    fun testPrivateKeySize() {
        val key = ECKey()

        val d = key.privKeyBytes

        assertEquals(32, d.size)
    }

    @Test
    fun testSignAndVerify() {

        for (i in 0..20) {

            val key = ECKey()

            val d = key.privKeyBytes

            val dBytes = fromUnsignedByteArray(d)

            val p = CryptoUtils.getPublicFor(d)
            val ecP = ECKey.publicKeyFromPrivate(dBytes, true)

            assertEquals(33, p.size)
            assertArrayEquals(ecP, p)

            val message = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89".decode16()

            val signature = BloqlySchnorr.sign(message, dBytes)

            assertEquals(64, signature.toByteArray().size)
            assertEquals(signature.toString(), Signature.fromString(signature.toString()).toString())

            assertTrue(BloqlySchnorr.verify(message, signature, p))
        }
    }

    @Test
    fun testVector1() {
        val m = "0000000000000000000000000000000000000000000000000000000000000000".decode16().pad()
        val d = "0000000000000000000000000000000000000000000000000000000000000001".toHexBigInteger()
        val p = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".decode16()
        val s =
            "787A848E71043D280C50470E8E1532B2DD5D20EE912A45DBDD2BD1DFBF187EF67031A98831859DC34DFFEEDDA86831842CCD0079E1F92AF177F7F22CC1DCED05"

        val signature = BloqlySchnorr.sign(m, d)

        assertEquals(s, signature.toString())
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertTrue(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector2() {
        val m = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89".decode16().pad()
        val d = "B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF".toHexBigInteger()
        val p = "02DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659".decode16()
        val s =
            "2A298DACAE57395A15D0795DDBFD1DCB564DA82B0F269BC70A74F8220429BA1D1E51A22CCEC35599B8F266912281F8365FFC2D035A230434A1A64DC59F7013FD"

        val signature = BloqlySchnorr.sign(m, d)

        assertEquals(s, signature.toString())
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertTrue(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector3() {
        val m = "5E2D58D8B3BCDF1ABADEC7829054F90DDA9805AAB56C77333024B9D0A508B75C".decode16().pad()
        val d = "C90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B14E5C7".toHexBigInteger()
        val p = "03FAC2114C2FBB091527EB7C64ECB11F8021CB45E8E7809D3C0938E4B8C0E5F84B".decode16()
        val s =
            "00DA9B08172A9B6F0466A2DEFD817F2D7AB437E0D253CB5395A963866B3574BE00880371D01766935B92D2AB4CD5C8A2A5837EC57FED7660773A05F0DE142380"

        val signature = BloqlySchnorr.sign(m, d)

        assertEquals(s, signature.toString())
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertTrue(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector4() {
        val m = "4DF3C3F68FCC83B27E9D42C90431A72499F17875C81A599B566C9889B9696703".decode16().pad()
        val p = "03DEFDEA4CDB677750A420FEE807EACF21EB9898AE79B9768766E4FAA04A2D4A34".decode16()
        val s =
            "00000000000000000000003B78CE563F89A0ED9414F5AA28AD0D96D6795F9C6302A8DC32E64E86A333F20EF56EAC9BA30B7246D6D25E22ADB8C6BE1AEB08D49D"

        val signature = Signature.fromString(s)
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertTrue(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector5() {
        val m = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89".decode16().pad()
        val p = "02DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659".decode16()
        val s =
            "2A298DACAE57395A15D0795DDBFD1DCB564DA82B0F269BC70A74F8220429BA1DFA16AEE06609280A19B67A24E1977E4697712B5FD2943914ECD5F730901B4AB7"

        val signature = Signature.fromString(s)
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertFalse(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector6() {
        val m = "5E2D58D8B3BCDF1ABADEC7829054F90DDA9805AAB56C77333024B9D0A508B75C".decode16().pad()
        val p = "03FAC2114C2FBB091527EB7C64ECB11F8021CB45E8E7809D3C0938E4B8C0E5F84B".decode16()
        val s =
            "00DA9B08172A9B6F0466A2DEFD817F2D7AB437E0D253CB5395A963866B3574BED092F9D860F1776A1F7412AD8A1EB50DACCC222BC8C0E26B2056DF2F273EFDEC"

        val signature = Signature.fromString(s)
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertFalse(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector7() {
        val m = "0000000000000000000000000000000000000000000000000000000000000000".decode16().pad()
        val p = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".decode16()
        val s =
            "787A848E71043D280C50470E8E1532B2DD5D20EE912A45DBDD2BD1DFBF187EF68FCE5677CE7A623CB20011225797CE7A8DE1DC6CCD4F754A47DA6C600E59543C"

        val signature = Signature.fromString(s)
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertFalse(BloqlySchnorr.verify(m, signature, p))
    }

    @Test
    fun testVector8() {
        val m = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89".decode16().pad()
        val p = "03DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659".decode16()
        val s =
            "2A298DACAE57395A15D0795DDBFD1DCB564DA82B0F269BC70A74F8220429BA1D1E51A22CCEC35599B8F266912281F8365FFC2D035A230434A1A64DC59F7013FD"

        val signature = Signature.fromString(s)
        assertEquals(s, Signature.fromString(signature.toString()).toString())

        assertFalse(BloqlySchnorr.verify(m, signature, p))
    }
}