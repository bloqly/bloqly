package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.vo.AccountVO

@ValueObject
data class Genesis(
    val root: Account,
    val validators: List<Account>,
    val users: List<Account>
)