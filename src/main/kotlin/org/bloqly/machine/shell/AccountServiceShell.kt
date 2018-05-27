package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Account
import org.bloqly.machine.service.AccountService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AccountServiceShell(
    private val accountService: AccountService,
    private val objectMapper: ObjectMapper) {

    private val log: Logger = LoggerFactory.getLogger(AccountServiceShell::class.simpleName)

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