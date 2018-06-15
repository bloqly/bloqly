package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class Genesis(
    val block: BlockVO,
    val transactions: List<TransactionVO>,
    val source: String
)