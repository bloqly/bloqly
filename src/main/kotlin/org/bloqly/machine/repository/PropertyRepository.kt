package org.bloqly.machine.repository

import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface PropertyRepository : CrudRepository<Property, PropertyId>, PropertyRepositoryCustom {

    @Query("select p from Property p where p.id.spaceId = ?1 and p.id.key = ?2")
    fun findBySpaceAndKey(space: String, key: String): List<Property>

    @Modifying
    @Query("update Property p set p.value = ?2 where p.id = ?1")
    fun update(id: PropertyId, value: ByteArray)
}
