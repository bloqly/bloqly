package org.bloqly.machine.repository

import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.PropertyResult
import org.bloqly.machine.model.PropertyValue
import org.bloqly.machine.model.ValueType
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PropertyService(private val propertyRepository: PropertyRepository) {

    fun updateProperties(properties: List<Property>) {
        propertyRepository.saveAll(properties)
    }

    fun updateProperties(spaceId: String, self: String, propertyResults: List<PropertyResult>) {
        val properties = propertyResults.map { it.toProperty(spaceId, self) }
        updateProperties(properties)
    }

    fun findById(propertyId: PropertyId): Property? = propertyRepository.findById(propertyId).orElse(null)

    fun getProperty(spaceId: String, self: String, target: String, key: String): PropertyValue {
        val valueBytes = propertyRepository.findById(PropertyId(spaceId, self, target, key)).orElse(null).value

        val value = ParameterUtils.readValue(valueBytes)

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