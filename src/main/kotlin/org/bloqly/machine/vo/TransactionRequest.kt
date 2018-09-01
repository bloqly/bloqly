package org.bloqly.machine.vo

import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.TransactionType

@ValueObject
data class TransactionRequest(
    var space: String = DEFAULT_SPACE,
    val origin: String,
    val passphrase: String,
    val destination: String,
    var transactionType: String = TransactionType.CALL.name,
    var self: String = DEFAULT_SELF,
    var key: String = DEFAULT_FUNCTION,
    val args: List<Arg>
)

data class Arg(
    val type: String,
    val value: String
)