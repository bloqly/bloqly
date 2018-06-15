package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote

@ValueObject
data class BlockData(

    val block: BlockVO,

    val transactions: List<TransactionVO>,

    val votes: List<VoteVO>
) {
    constructor(
        block: Block,
        transactions: List<Transaction>,
        votes: List<Vote>
    ) : this(block.toVO(), transactions.map { it.toVO() }, votes.map { it.toVO() })
}