package org.bloqly.machine.model

import org.bloqly.machine.repository.PropertyRepository

data class PropertyContext(val propertyRepository: PropertyRepository) {
    private val properties = mutableMapOf<PropertyId, ByteArray>()

    private fun syncPropertyValue(propertyId: PropertyId) {
        if (!properties.containsKey(propertyId)) {
            propertyRepository.findById(propertyId).ifPresent { property ->
                properties[propertyId] = property.value
            }
        }
    }

    private fun getPropertyId(spaceId: String, self: String, target: String, key: String): PropertyId =
        PropertyId(
            spaceId = spaceId,
            self = self,
            target = target,
            key = key
        )

    fun getPropertyValue(spaceId: String, self: String, target: String, key: String): ByteArray? {

        val propertyId = getPropertyId(spaceId, self, target, key)

        syncPropertyValue(propertyId)

        return properties[propertyId]
    }

    fun updatePropertyValue(spaceId: String, self: String, target: String, key: String, value: ByteArray) {
        properties[getPropertyId(spaceId, self, target, key)] = value
    }
}