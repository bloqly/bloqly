package org.bloqly.machine.vo.transaction

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.TransactionOutput

@ValueObject
data class TransactionOutputVO(
    val blockHash: String,
    val transactionHash: String,
    val output: String
) {
    fun toModel(): TransactionOutput =
        TransactionOutput(
            blockHash = blockHash,
            transactionHash = transactionHash,
            output = output
        )
}