package org.bloqly.machine.repository

import org.bloqly.machine.model.Transaction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransactionRepository : CrudRepository<Transaction, String> {

    @Query(
        """
        select t.*
        from transaction t
        inner join block b on b.hash = t.referenced_block_hash
        left outer join block_transactions bt on bt.transaction_id = t.id
        where
        t.space_id = ?1 and
        t.referenced_block_hash = ?2 and
        t.timestamp > ?3 and
        b.height >= ?4 and
        bt.block_id is null
        order by t.timestamp asc
    """, nativeQuery = true
    )
    fun findPendingTransactionsBySpaceId(
        spaceId: String,
        libHash: String,
        minTimestamp: Long,
        minHeight: Long
    ): List<Transaction>

    fun existsByHash(hash: String): Boolean
}