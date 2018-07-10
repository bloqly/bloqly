package org.bloqly.machine.model

import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.service.ContractService

data class PropertyContext(
    val propertyService: PropertyService,
    val contractService: ContractService
) {
    private val properties = mutableMapOf<PropertyId, Property>()
    private val contracts = mutableMapOf<String, Contract>()

    private fun syncPropertyValue(propertyId: PropertyId) {
        if (!properties.containsKey(propertyId)) {
            propertyService.findById(propertyId)?.let { property ->
                properties[propertyId] = property
            }
        }
    }

    private fun syncContract(self: String) {
        if (!contracts.containsKey(self)) {
            contractService.findById(self)?.let { contract ->
                contracts[self] = contract
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

        return properties[propertyId]?.value
    }

    fun getContract(self: String): Contract? {

        syncContract(self)

        return contracts[self]
    }

    fun saveContract(contract: Contract) {
        contracts[contract.id] = contract
    }

    fun updatePropertyValue(spaceId: String, self: String, target: String, key: String, value: ByteArray) {
        val propertyId = getPropertyId(spaceId, self, target, key)
        properties[propertyId] = Property(
            id = propertyId,
            value = value
        )
    }

    fun commit() {
        propertyService.updateProperties(properties.values.toList())
        contractService.saveAll(contracts.values.toList())
    }
}