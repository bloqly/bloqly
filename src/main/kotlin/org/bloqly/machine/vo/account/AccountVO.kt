package org.bloqly.machine.vo.account

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Account

@ValueObject
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountVO(
    val accountId: String,
    val publicKey: String,
    val privateKeyEncrypted: String? = null
) {
    fun toModel(): Account =
        Account(
            accountId = accountId,
            publicKey = publicKey,
            privateKeyEncrypted = privateKeyEncrypted
        )
}