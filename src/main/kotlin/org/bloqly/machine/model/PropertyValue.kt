package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject

@ValueObject
data class PropertyValue(
    val value: String,
    val type: ValueType
)