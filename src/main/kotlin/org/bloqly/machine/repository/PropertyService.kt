package org.bloqly.machine.repository

import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Service

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository
) {
    fun saveGenesis(propertyId: PropertyId, source: String) {

        require(!propertyRepository.existsById(propertyId)) {
            "Genesis for key $propertyId already exists."
        }

        propertyRepository.save(Property(id = propertyId, value = ParameterUtils.writeValue(source)))
    }
}