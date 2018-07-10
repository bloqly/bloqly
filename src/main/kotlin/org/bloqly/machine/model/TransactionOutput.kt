package org.bloqly.machine.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class TransactionOutput(
    @EmbeddedId
    val transactionOutputId: TransactionOutputId,
    val output: String
)