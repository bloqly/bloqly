package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Block

@ValueObject
data class BlockData(

    val block: BlockVO,

    val transactions: List<TransactionVO>,

    val votes: List<VoteVO>

) {
    constructor(
        block: Block
    ) : this(block.toVO(), block.transactions.map { it.toVO() }, block.votes.map { it.toVO() })
}