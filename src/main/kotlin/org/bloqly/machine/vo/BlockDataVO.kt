package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.BlockData

@ValueObject
data class BlockDataVO(
    val block: BlockVO,
    val transactions: List<TransactionVO>,
    val votes: List<VoteVO>
) {

    fun toModel(): BlockData {

        return BlockData(
                block = block.toModel(),
                transactions = transactions.map { it.toModel() },
                votes = votes.map { it.toModel() }
        )
    }
}