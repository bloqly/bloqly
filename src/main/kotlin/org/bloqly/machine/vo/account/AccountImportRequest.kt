package org.bloqly.machine.vo.account

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.annotation.ValueObject

@ApiModel
@ValueObject
data class AccountImportRequest(
    @ApiModelProperty("Account private key in hex format")
    val privateKey: String,
    @ApiModelProperty("Password to encrypt private key before storing in database")
    val password: String
)