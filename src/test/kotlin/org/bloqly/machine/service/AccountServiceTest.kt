package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Space
import org.bloqly.machine.test.TestService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        val validator1 = accountService.getProducerBySpace(space, 0)
        val validator2 = accountService.getProducerBySpace(space, 1)
        val validator3 = accountService.getProducerBySpace(space, 2)
        val validator4 = accountService.getProducerBySpace(space, 3)
        val validator5 = accountService.getProducerBySpace(space, 4)

        assertNotNull(validator1)
        assertNotNull(validator2)
        assertNotNull(validator3)
        assertNotNull(validator4)
        assertNotNull(validator5)

        assertNotEquals(validator1, validator2)
        assertNotEquals(validator2, validator3)
        assertNotEquals(validator3, validator2)
        assertNotEquals(validator4, validator3)

        assertEquals(validator5, validator1)
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