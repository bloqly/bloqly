package org.bloqly.machine.service

import junit.framework.Assert.fail
import org.bloqly.machine.Application
import org.bloqly.machine.exception.AccountAlreadyExistsException
import org.bloqly.machine.model.Account
import org.bloqly.machine.test.TestService
import org.junit.After
import org.junit.Assert
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

    @Before
    fun setup() {
        account = accountService.newAccount()

        accountService.importAccount(account.privateKey!!)
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testNewAccount() {

        for (i in 0..4) {

            val account = accountService.newAccount();

            println("id: ${account.id}")
            println("pub: ${account.publicKey}")
            println("priv: ${account.privateKey}")
        }
    }

    @Test
    fun testImportAccountTwiceFails() {
        try {
            accountService.importAccount(account.privateKey!!)
            fail()
        } catch (e: Exception) {
            Assert.assertTrue(e.cause is AccountAlreadyExistsException)
        }
    }

}