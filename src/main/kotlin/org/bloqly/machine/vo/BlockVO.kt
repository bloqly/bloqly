package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

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
)