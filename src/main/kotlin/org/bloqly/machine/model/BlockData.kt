package org.bloqly.machine.model

import org.bloqly.machine.vo.BlockDataVO

data class BlockData(

    val block: Block,

    val transactions: List<Transaction>,

    val votes: List<Vote>) {

    fun toVO(): BlockDataVO {

        return BlockDataVO(
                block = block.toVO(),
                transactions = transactions.map { it.toVO() },
                votes = votes.map { it.toVO() }
        )
    }
}