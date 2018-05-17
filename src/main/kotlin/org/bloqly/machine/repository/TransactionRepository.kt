package org.bloqly.machine.repository

import org.bloqly.machine.model.Transaction
import org.springframework.data.repository.CrudRepository

interface TransactionRepository : CrudRepository<Transaction, String> {

    fun findBySpaceAndContainingBlockIdIsNull(space: String): List<Transaction>

    fun findByContainingBlockIdIsNull(): List<Transaction>

}