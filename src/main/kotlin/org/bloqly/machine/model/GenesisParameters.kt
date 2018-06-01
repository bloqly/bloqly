package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisParameters(
    val root: Account,
    val validators: List<Account>? = null,
    val users: List<Account>? = null,
    var source: String
)