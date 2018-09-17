package org.bloqly.machine.vo.account

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.annotation.ValueObject

@ApiModel
@ValueObject
data class AccountImportRequest(
    @ApiModelProperty("Account public key")
    val publicKey: String,
    @ApiModelProperty("Encrypted account private key in hex format")
    val privateKeyEncrypted: String
)