package org.bloqly.machine.vo.transaction

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.TransactionOutput
import org.bloqly.machine.vo.property.PropertyValue

@ValueObject
data class TransactionOutputVO(
    val blockHash: String,
    val transactionHash: String,
    val output: List<PropertyValue>
) {
    fun toModel(): TransactionOutput =
        TransactionOutput(
            blockHash = blockHash,
            transactionHash = transactionHash,
            output = output
        )
}