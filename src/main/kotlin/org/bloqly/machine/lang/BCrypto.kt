package org.bloqly.machine.lang

import org.bloqly.machine.crypto.CryptoUtils
import org.bloqly.machine.util.fromHex
import org.bloqly.machine.util.toHex

class BCrypto {

    companion object {

        @JvmStatic
        fun sha256(input: String): String =
            CryptoUtils.hash(input.toByteArray()).toHex()

        @JvmStatic
        fun verify(message: String, signature: String, publicKey: String): Boolean =
            CryptoUtils.verify(
                message = message.fromHex(),
                signature = signature.fromHex(),
                publicKey = publicKey.fromHex()
            )

        @JvmStatic
        fun verifySchnorr(message: String, signature: String, publicKey: String): Boolean =
            CryptoUtils.verifySchnorrHex(
                message = message,
                signature = signature,
                publicKey = publicKey
            )
    }
}