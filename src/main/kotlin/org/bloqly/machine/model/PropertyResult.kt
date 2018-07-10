package org.bloqly.machine.model

import org.bloqly.machine.util.ParameterUtils

data class PropertyResult(
    val target: String,
    val key: String,
    val value: Any
) {
    fun toProperty(spaceId: String, self: String): Property = Property(
        PropertyId(
            spaceId = spaceId,
            self = self,
            target = target,
            key = key
        ),
        value = ParameterUtils.writeValue(value)
    )
}