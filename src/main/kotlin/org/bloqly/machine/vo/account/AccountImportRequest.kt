package org.bloqly.machine.vo.account

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class AccountImportRequest(
    val privateKey: String,
    val password: String
)