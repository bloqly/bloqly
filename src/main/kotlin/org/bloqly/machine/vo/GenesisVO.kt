package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class GenesisVO(
    val root: AccountVO,
    val validators: List<AccountVO>,
    val users: List<AccountVO>
)