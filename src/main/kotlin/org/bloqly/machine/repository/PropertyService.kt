package org.bloqly.machine.repository

import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.PropertyResult
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

    fun getNonce(spaceId: String,  accountId: String) {


    }
}