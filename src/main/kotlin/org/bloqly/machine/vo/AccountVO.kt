package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class AccountVO(
    val id: String,
    val publicKey: String,
    val privateKey: String
)