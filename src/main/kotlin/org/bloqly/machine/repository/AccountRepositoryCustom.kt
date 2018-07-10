package org.bloqly.machine.repository

interface AccountRepositoryCustom {
    fun insertAccountIdIfNotExists(accountId: String?)
}