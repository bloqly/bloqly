package org.bloqly.machine.vo.genesis

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.vo.transaction.TransactionVO
import org.bloqly.machine.vo.block.BlockVO

@ValueObject
data class Genesis(
    val block: BlockVO,
    val transactions: List<TransactionVO>
)