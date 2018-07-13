package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Block

@ValueObject
data class BlockVO(
    val spaceId: String,
    val height: Long,
    val weight: Long,
    val diff: Int,
    val round: Long,
    val timestamp: Long,
    val parentHash: String,
    val producerId: String,
    val txHash: String?,
    val validatorTxHash: String,
    val signature: String,
    val hash: String,
    val libHash: String
) {

    fun toModel(): Block {

        return Block(
            spaceId = spaceId,
            height = height,
            weight = weight,
            diff = diff,
            round = round,
            timestamp = timestamp,
            parentHash = parentHash,
            producerId = producerId,
            txHash = txHash,
            validatorTxHash = validatorTxHash,
            signature = signature,
            hash = hash,
            libHash = libHash
        )
    }
}