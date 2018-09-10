package org.bloqly.machine.component

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.lang.BLong
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.service.PropertyService
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.util.ParameterUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class, EventSenderServiceTest.TestConfiguration::class])
class PropertyContextTest {

    @Autowired
    private lateinit var propertyService: PropertyService

    @Autowired
    private lateinit var contractService: ContractService

    private lateinit var propertyContext: PropertyContext

    private lateinit var propertyId: PropertyId

    @Before
    fun setup() {
        propertyContext = PropertyContext(propertyService, contractService)

        propertyId = PropertyId(DEFAULT_SPACE, DEFAULT_SELF, "userId", "balance")
    }

    @Test
    fun testPropertyOverrides() {

        propertyContext.updatePropertyValues(
            listOf(
                Property(propertyId, ParameterUtils.writeValue("BigInteger(1)"))
            )
        )

        propertyContext.updatePropertyValues(
            listOf(
                Property(propertyId, ParameterUtils.writeValue("BigInteger(2)"))
            )
        )

        val property = propertyContext.properties.first()

        assertEquals(BLong("2"), ParameterUtils.readValue(property.value))
    }
}