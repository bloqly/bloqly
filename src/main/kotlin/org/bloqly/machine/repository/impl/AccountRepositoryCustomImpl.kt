package org.bloqly.machine.repository.impl

import org.bloqly.machine.model.Account
import org.bloqly.machine.repository.AccountRepositoryCustom
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
class AccountRepositoryCustomImpl(
    private var entityManager: EntityManager
) : AccountRepositoryCustom {

    override fun insertAccountIdIfNotExists(accountId: String?) {

        if (accountId == null) {
            return
        }

        val account = entityManager.find(Account::class.java, accountId)

        if (account == null) {
            entityManager.persist(Account(accountId))
        }
    }
}