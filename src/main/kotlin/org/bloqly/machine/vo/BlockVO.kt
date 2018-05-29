package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Block

@ValueObject
data class BlockVO(
    val id: String,
    val space: String,
    val height: Long,
    val timestamp: Long,
    val parentHash: String,
    val proposerId: String,
    val txHash: String,
    val validatorTxHash: String,
    val signature: String
) {

    fun toModel(): Block {

        return Block(
                id = id,
                space = space,
                height = height,
                timestamp = timestamp,
                parentHash = parentHash,
                proposerId = proposerId,
                txHash = txHash.toByteArray(),
                validatorTxHash = validatorTxHash.toByteArray(),
                signature = signature.toByteArray()
        )
    }
}