package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.PropertyResult
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.vo.property.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository
) {

    @Transactional
    fun updateProperties(properties: List<Property>) {
        propertyRepository.saveAll(properties)
    }

    @Transactional
    fun updateProperties(spaceId: String, self: String, propertyResults: List<PropertyResult>) {
        val properties = propertyResults.map { it.toProperty(spaceId, self) }
        updateProperties(properties)
    }

    @Transactional(readOnly = true)
    fun findById(propertyId: PropertyId): Property? = propertyRepository.findById(propertyId).orElse(null)

    @Transactional(readOnly = true)
    fun getPropertyValue(spaceId: String, self: String, target: String, key: String): Value? {
        return propertyRepository.findById(PropertyId(spaceId, self, target, key))
            .map { it.value }.orElse(null)
    }

    @Transactional(readOnly = true)
    fun getPropertyValue(target: String, key: String): Value? {
        return propertyRepository.findById(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, target, key))
            .map { it.value }.orElse(null)
    }
}