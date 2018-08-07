package org.bloqly.machine.controller.admin.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class AccountImportRequest(
    val privateKey: String,
    val password: String
)