package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class VoteVO(
    val validatorId: String,
    val space: String,
    val height: Long,
    val blockId: String,
    val timestamp: Long,
    val signature: String
)
