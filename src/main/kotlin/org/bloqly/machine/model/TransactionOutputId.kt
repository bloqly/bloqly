package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
data class TransactionOutputId(
    val blockId: String,
    val transactionId: String
) : Serializable