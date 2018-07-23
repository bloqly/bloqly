package org.bloqly.machine.test

import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.model.Account
import org.springframework.beans.factory.annotation.Autowired

open class BaseTest {

    @Autowired
    protected lateinit var passphraseService: PassphraseService

    @Autowired
    protected lateinit var testService: TestService

    fun passphrase(accountId: String): String = passphraseService.getPassphrase(accountId)

    fun passphrase(account: Account): String = passphraseService.getPassphrase(account.accountId)

    fun validator(n: Int) = testService.getValidator(n)

    fun passphrase(n: Int) = passphrase(validator(n))
}