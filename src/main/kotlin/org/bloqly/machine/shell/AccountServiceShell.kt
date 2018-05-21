package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.service.AccountService
import org.springframework.stereotype.Service

@Service
class AccountServiceShell(

    private val accountService: AccountService,
    private val objectWriter: ObjectWriter

) {

    fun new(): String {

        val account = accountService.newAccount()

        return objectWriter.writeValueAsString(account)
    }

    fun validators(space: String): String {

        val validators = accountService.getValidatorsForSpace(space)

        validators.forEach { validator ->

            validator.privateKey?.let {
                validator.privateKey = "hidden"
            }
        }

        return "\n" + objectWriter.writeValueAsString(validators)
    }

}