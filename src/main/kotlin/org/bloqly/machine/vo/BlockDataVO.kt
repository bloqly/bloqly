package org.bloqly.machine.vo

data class BlockDataVO(
        val block: BlockVO,
        val transactions: List<TransactionVO>,
        val votes: List<VoteVO>
)