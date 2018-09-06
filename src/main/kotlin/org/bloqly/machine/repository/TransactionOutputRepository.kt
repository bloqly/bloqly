package org.bloqly.machine.repository

import org.bloqly.machine.model.TransactionOutput
import org.springframework.data.repository.CrudRepository

interface TransactionOutputRepository : CrudRepository<TransactionOutput, Long> {
    fun findByBlockHash(hash: String): List<TransactionOutput>
    fun getByBlockHashAndTransactionHash(blockHash: String, transactionHash: String): TransactionOutput
}