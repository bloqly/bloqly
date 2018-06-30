package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Block

@ValueObject
data class BlockVO(
    val id: String,
    val spaceId: String,
    val height: Long,
    val round: Long,
    val timestamp: Long,
    val parentHash: String,
    val proposerId: String,
    val txHash: String?,
    val validatorTxHash: String?,
    val signature: String?
) {

    fun toModel(): Block {

        return Block(
            id = id,
            spaceId = spaceId,
            height = height,
            round = round,
            timestamp = timestamp,
            parentId = parentHash,
            proposerId = proposerId,
            txHash = txHash?.toByteArray(),
            validatorTxHash = validatorTxHash?.toByteArray(),
            signature = signature?.toByteArray()
        )
    }
}