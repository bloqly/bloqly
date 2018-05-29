package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Transaction

@ValueObject
data class TransactionListVO(val transactions: List<TransactionVO>) {

    companion object {

        fun fromTransactions(transactions: List<Transaction>): TransactionListVO {
            return TransactionListVO(transactions = transactions.map { it.toVO() })
        }
    }
}