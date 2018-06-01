package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisVO(
    val block: BlockVO,
    val transactions: List<TransactionVO>,
    val source: String
)