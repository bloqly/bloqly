package org.bloqly.machine.model

data class BlockData(

    val block: Block,

    val transactions: List<Transaction>,

    val votes: List<Vote>
)