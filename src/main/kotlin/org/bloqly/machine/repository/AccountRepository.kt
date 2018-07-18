package org.bloqly.machine.repository

import org.bloqly.machine.model.Account
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface AccountRepository : CrudRepository<Account, String> {

    fun findValidatorByPublicKey(publicKey: String): Account

    fun existsByAccountId(accountId: String): Boolean

    fun findByAccountId(originId: String): Account?

    @Query("from Account where accountId in ?1")
    fun findAllByAccountIds(accountIds: List<String>): List<Account>
}