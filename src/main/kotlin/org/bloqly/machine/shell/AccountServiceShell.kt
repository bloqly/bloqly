package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Account
import org.bloqly.machine.service.AccountService
import org.springframework.stereotype.Service

@Service
@Suppress("unused")
class AccountServiceShell(
    private val accountService: AccountService,
    private val objectMapper: ObjectMapper
) {

    fun new(): String {

        val account = accountService.newAccount()

        return objectMapper.writeValueAsString(account)
    }

    fun validators(space: String): String {

        val validators = accountService.getValidatorsForSpace(space)

        validators.forEach { validator ->

            validator.privateKey?.let {
                validator.privateKey = "hidden"
            }
        }

        return "\n" + objectMapper.writeValueAsString(validators)
    }

    fun import(privateKey: String): String {

        accountService.importAccount(privateKey)

        return "OK"
    }

    @ValueObject
    data class Accounts(
        val accounts: List<Account>
    )
}