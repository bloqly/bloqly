package org.bloqly.machine.lang

import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16

class BCrypto {

    companion object {

        @JvmStatic
        fun sha256(input: String): String =
            CryptoUtils.hash(input.toByteArray()).encode16()

        @JvmStatic
        fun verify(message: String, signature: String, publicKey: String): Boolean =
            CryptoUtils.verify(
                message = message.decode16(),
                signature = signature.decode16(),
                publicKey = publicKey.decode16()
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