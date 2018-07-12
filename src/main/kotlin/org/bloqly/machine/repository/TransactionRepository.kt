package org.bloqly.machine.repository

import org.bloqly.machine.model.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransactionRepository : CrudRepository<Transaction, String> {

    @Query("select t from Transaction t where t.containingBlockId is null and t.timestamp > ?1")
    fun findPendingTransactions(minTimestamp: Long): List<Transaction>

    @Query("select t from Transaction t where t.spaceId = ?1 and t.containingBlockId is null and t.timestamp > ?2")
    fun findPendingTransactionsBySpaceId(spaceId: String, minTimestamp: Long): List<Transaction>
}