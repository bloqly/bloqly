package org.bloqly.machine.vo.account

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.annotation.ValueObject

@ApiModel
@ValueObject
data class PublicKeysImportRequest(
    @ApiModelProperty("List of public keys")
    val publicKeys: List<String>
)