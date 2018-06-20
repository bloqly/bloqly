package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Space
import org.bloqly.machine.test.TestService
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class AccountServiceTest {

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var testService: TestService

    private lateinit var account: Account

    private lateinit var space: Space

    @Before
    fun setup() {
        testService.cleanup()
        testService.createBlockchain()

        account = accountService.newAccount()

        space = testService.getDefaultSpace()

        accountService.importAccount(account.privateKey!!)
    }

    @Test
    fun testGetActiveValidator() {
        assertNotNull(accountService.getActiveProducerBySpace(space))
    }

    @Test
    fun testNewAccount() {

        for (i in 0..4) {

            val account = accountService.newAccount()

            println("id: ${account.id}")
            println("pub: ${account.publicKey!!.toLowerCase()}")
            println("priv: ${account.privateKey}")
        }
    }

    @Test
    fun testImportAccountTwiceFails() {
        try {
            accountService.importAccount(account.privateKey!!)
            fail()
        } catch (e: Exception) {
        }
    }
}