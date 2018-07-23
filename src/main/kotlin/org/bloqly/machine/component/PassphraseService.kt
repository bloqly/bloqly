package org.bloqly.machine.component

import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class PassphraseService(private val env: Environment) {

    companion object {
        private const val PASSPHRASE_PREFIX = "keys.passphrase_"

        private const val PASSPHRASE_SUFFIX_LENGTH = 8
    }

    private fun getPassphraseKey(accountId: String): String =
        PASSPHRASE_PREFIX + accountId.substring(0, PASSPHRASE_SUFFIX_LENGTH)

    fun getPassphrase(accountId: String): String =
        env.getRequiredProperty(getPassphraseKey(accountId))

    fun hasPassphrase(accountId: String): Boolean =
        env.containsProperty(getPassphraseKey(accountId))
}