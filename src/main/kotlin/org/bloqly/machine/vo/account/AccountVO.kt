package org.bloqly.machine.vo.account

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class AccountVO(
    val accountId: String,
    val publicKey: String,
    val privateKeyEncoded: String? = null
)