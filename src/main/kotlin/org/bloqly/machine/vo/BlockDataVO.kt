package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class BlockDataVO(
    val block: BlockVO,
    val transactions: List<TransactionVO>,
    val votes: List<VoteVO>
)