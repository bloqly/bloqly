package org.bloqly.machine.component

import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.service.PropertyService
import org.bloqly.machine.service.ContractService

data class PropertyContext(
    val propertyService: PropertyService,
    val contractService: ContractService
) {

    private val _properties = mutableMapOf<PropertyId, Property>()

    val properties: List<Property>
        get() = _properties.values.toList()

    private val _contracts = mutableMapOf<String, Contract>()

    private fun syncPropertyValue(propertyId: PropertyId) {
        if (!_properties.containsKey(propertyId)) {
            propertyService.findById(propertyId)?.let { property ->
                _properties[propertyId] = property
            }
        }
    }

    private fun syncContract(self: String) {
        if (!_contracts.containsKey(self)) {
            contractService.findById(self)?.let { contract ->
                _contracts[self] = contract
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

        return _properties[propertyId]?.value
    }

    fun getContract(self: String): Contract? {

        syncContract(self)

        return _contracts[self]
    }

    fun saveContract(contract: Contract) {
        _contracts[contract.id] = contract
    }

    private fun updatePropertyValue(property: Property) {
        _properties[property.id] = property
    }

    /**
     * To call only when create a new blockchain or import genesis block
     */
    fun commit() {
        propertyService.updateProperties(_properties.values.toList())
        contractService.saveAll(_contracts.values.toList())
    }

    fun updatePropertyValues(properties: List<Property>) =
        properties.forEach { updatePropertyValue(it) }

    fun merge(propertyContext: PropertyContext) {
        _properties.putAll(propertyContext._properties)
        _contracts.putAll(propertyContext._contracts)
    }

    fun getLocalCopy(): PropertyContext {
        val propertyContext = PropertyContext(propertyService, contractService)

        propertyContext.updatePropertyValues(_properties.values.toList())

        _contracts.values.forEach { propertyContext.saveContract(it) }

        return propertyContext
    }
}