package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.vo.property.Value
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class PropertyServiceTest : BaseTest() {

    @Test
    fun testPropertyService() {
        val id = PropertyId(
            spaceId = "main",
            self = "self",
            target = "accountId",
            key = "balance"
        )

        val property = Property(
            id = id,
            value = Value.of(1)
        )

        propertyService.updateProperties(listOf(property))

        val savedProperty = propertyService.findById(id)

        assertEquals(property, savedProperty)

        val newProperty = Property(
            id = id,
            value = Value.of(2)
        )

        propertyService.updateProperties(listOf(newProperty))

        val savedNewProperty = propertyService.findById(id)

        assertEquals(newProperty, savedNewProperty)
    }
}