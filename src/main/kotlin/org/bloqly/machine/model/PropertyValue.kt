package org.bloqly.machine.model

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.util.ParameterUtils

@ValueObject
data class PropertyValue(
    val value: String,
    val type: ValueType
) {
    companion object {

        fun of(propertyValue: ByteArray): PropertyValue {
            val value = ParameterUtils.readValue(propertyValue)

            val valueType = when (value) {
                is Int -> ValueType.INT
                is BInteger -> ValueType.BIGINT
                is Boolean -> ValueType.BOOLEAN
                is String -> ValueType.STRING
                else -> throw IllegalArgumentException("Could not detect type of property value $value")
            }

            return PropertyValue(value.toString(), valueType)
        }
    }
}