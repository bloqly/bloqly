package org.bloqly.machine.vo.block

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.annotation.ValueObject

@ApiModel
@ValueObject
data class BlockRangeRequest(
    @ApiModelProperty(value = "Space id, default value is 'main'", example = "main")
    val spaceId: String,
    @ApiModelProperty(value = "Start height to filter blocks from", example = "0")
    var startHeight: Long = 0,
    @ApiModelProperty(value = "End height to filter blocks to", example = "3")
    var endHeight: Long = 0
)