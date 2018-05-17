package org.bloqly.machine.vo

import org.bloqly.machine.model.TransactionType

data class TransactionVO(
    val id: String,
    val space: String,
    val destination: String,
    val self: String?,
    val key: String?,
    val value: String,
    val transactionType: TransactionType,
    val referencedBlockId: String,
    val timestamp: Long,
    val signature: String,
    val publicKey: String
)