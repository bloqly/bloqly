package org.bloqly.machine.repository

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.model.GenesisParameters
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PropertyService(
    private val propertyRepository: PropertyRepository
) {
    fun saveGenesis(propertyId: PropertyId, source: String) {

        require(!propertyRepository.existsById(propertyId)) {
            "Genesis for key $propertyId already exists."
        }

        propertyRepository.save(Property(id = propertyId, value = ParameterUtils.writeValue(source)))
    }

    fun updateProperties(properties: List<Property>) {

        properties.forEach { propertyRepository.save(it) }
    }

    fun updateProperties(parametersContainer: GenesisParameters) {

        val properties = parametersContainer.parameters
            .map { (target, key, value) ->
                Property(
                    id = PropertyId(
                        space = DEFAULT_SPACE,
                        self = DEFAULT_SELF,
                        target = target,
                        key = key
                    ),
                    value = ParameterUtils.writeValue(value)
                )
            }

        properties.forEach { propertyRepository.save(it) }
    }
}