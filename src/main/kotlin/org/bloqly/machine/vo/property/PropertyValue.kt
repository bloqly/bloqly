package org.bloqly.machine.vo.property

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.Application
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import java.io.Serializable

@ApiModel
@ValueObject
data class PropertyValue(

    @ApiModelProperty(value = "Space id, default value is 'main'", example = "main")
    var space: String = Application.DEFAULT_SPACE,

    @ApiModelProperty(value = "Represent the contract address to which this property belongs", example = "self")
    var self: String = Application.DEFAULT_SELF,

    @ApiModelProperty(value = "The property name", example = "quorum")
    val key: String,

    @ApiModelProperty(value = "Represent the address to which this property belongs", example = "self")
    val target: String,

    @ApiModelProperty("Value")
    val value: Value
) : Serializable {
    fun toProperty(): Property =
        Property(
            id = PropertyId(
                spaceId = space,
                self = self,
                target = target,
                key = key
            ),
            value = value
        )
}