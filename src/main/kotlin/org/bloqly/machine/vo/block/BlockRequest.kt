package org.bloqly.machine.vo.block

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.annotation.ValueObject

@ApiModel("Simple block request")
@ValueObject
data class BlockRequest(
    @ApiModelProperty(value="Space id, default value is 'main'", example = "main")
    val spaceId: String
)