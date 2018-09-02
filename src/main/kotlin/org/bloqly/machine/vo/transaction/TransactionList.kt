package org.bloqly.machine.vo.transaction

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Transaction

@ValueObject
data class TransactionList(val transactions: List<TransactionVO>) {

    companion object {

        fun fromTransactions(transactions: List<Transaction>): TransactionList {
            return TransactionList(transactions.map { it.toVO() })
        }
    }
}