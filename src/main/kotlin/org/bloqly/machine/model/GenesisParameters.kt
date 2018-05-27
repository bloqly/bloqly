package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisParameters(
    val root: Account,
    var validators: List<Account>? = null,
    var users: List<Account>? = null
)