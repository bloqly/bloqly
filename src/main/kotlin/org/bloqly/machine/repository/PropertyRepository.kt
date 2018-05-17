package org.bloqly.machine.repository

import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface PropertyRepository : CrudRepository<Property, PropertyId> {

    @Query("select p from Property p where p.id.space = ?1 and p.id.key = ?2")
    fun findBySpaceAndKey(space: String, key: String): List<Property>

    @Query("select p from Property p where p.id.key = ?1")
    fun findByKey(key: String): List<Property>

}
