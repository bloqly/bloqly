package org.bloqly.machine.service

import org.junit.Test
import org.junit.runner.RunWith
import org.bloqly.machine.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class AccountServiceTest {

    @Autowired
    private lateinit var accountService: AccountService

    @Test
    fun testNewAccount() {

        for (i in 0..4) {

            val account = accountService.newAccount();

            println("id: ${account.id}")
            println("pub: ${account.publicKey}")
            println("priv: ${account.privateKey}")
        }
    }

}