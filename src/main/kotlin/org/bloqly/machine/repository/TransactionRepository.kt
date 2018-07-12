package org.bloqly.machine.repository

import org.bloqly.machine.model.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransactionRepository : CrudRepository<Transaction, String> {

    @Query(
        """
        select t.*
        from transaction t
        where
        t.timestamp > ?2 and
    """, nativeQuery = true
    )
    fun findRecentTransactions(minTimestamp: Long): List<Transaction>

    @Query(
        """
        select t.*
        from transaction t
        left outer join block_transactions bt on bt.transaction_id = t.id
        where
        t.space_id = ?1 and
        t.referenced_block_hash = ?2 and
        t.timestamp > ?3 and
        bt.block_id is null
    """, nativeQuery = true
    )
    fun findPendingTransactionsBySpaceId(spaceId: String, libHash: String, minTimestamp: Long): List<Transaction>

    fun existsByHash(hash: String): Boolean
}