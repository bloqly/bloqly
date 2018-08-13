package org.bloqly.machine.repository

import org.bloqly.machine.model.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransactionRepository : CrudRepository<Transaction, String> {

    fun existsByHash(hash: String): Boolean

    fun existsByNonce(nonce: String): Boolean

    @Query(
        """
        select t.* from block_transactions bt
        inner join transaction t on t.id = bt.transaction_id
        where bt.block_id in ?1
        """, nativeQuery = true
    )
    fun getTransactionsByBlockIds(blockIds: List<Long>): List<Transaction>

    // TODO we'll need some optimizations here
    @Query(
        """
        select t.* from transaction t
        where
        t.space_id = ?1 and
        t.id not in (
            select ft.transaction_id from finalized_transaction ft
        )
        """, nativeQuery = true
    )
    fun getPendingTransactionsBySpace(spaceId: String): List<Transaction>
}