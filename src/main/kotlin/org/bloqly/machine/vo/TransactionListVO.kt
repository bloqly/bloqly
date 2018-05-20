package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class TransactionListVO(
    val transactions: List<TransactionVO>
)