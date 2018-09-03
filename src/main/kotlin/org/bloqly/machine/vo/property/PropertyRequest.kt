package org.bloqly.machine.vo.property

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject

@ApiModel
@ValueObject
data class PropertyRequest(
    @ApiModelProperty(value = "Space id, default value is 'main'", example = "main")
    var space: String = DEFAULT_SPACE,
    @ApiModelProperty(value = "Represent the contract address to which this property belongs", example = "self")
    var self: String = DEFAULT_SELF,
    @ApiModelProperty(value = "The property name", example = "quorum")
    val key: String,
    @ApiModelProperty(value = "Represent the address to which this property belongs", example = "self")
    val target: String,
    @ApiModelProperty(value = "Whether request should return the latest value, or only finalized", example = "false")
    var finalized: Boolean = false
)