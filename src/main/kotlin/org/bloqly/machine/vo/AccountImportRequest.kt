package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class AccountImportRequest(
    val privateKey: String,
    val password: String
)