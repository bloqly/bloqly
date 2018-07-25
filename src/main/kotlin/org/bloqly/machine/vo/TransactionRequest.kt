package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class TransactionRequest(
    val space: String,
    val origin: String,
    val passphrase: String,
    val destination: String,
    val transactionType: String,
    val self: String,
    val key: String,
    val args: List<Arg>
)

data class Arg(
    val type: String,
    val value: String
)