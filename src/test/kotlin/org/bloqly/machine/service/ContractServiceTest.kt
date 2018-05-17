package org.bloqly.machine.service


import org.assertj.core.util.Sets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION_NAME
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.ParameterUtils.writeBoolean
import org.bloqly.machine.util.ParameterUtils.writeInteger
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.ParameterUtils.writeString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class ContractServiceTest {

    @Autowired
    private lateinit var contractService: ContractService

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    @Autowired
    private lateinit var testService: TestService

    private val creator = "owner id"

    private val caller = "caller id"

    private val callee = "callee id"

    @After
    fun tearDown() {

        testService.cleanup()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateContractWithEmptyBodyFails() {
        contractService.createContract(DEFAULT_SPACE, DEFAULT_SELF, creator, "")
    }

    @Test
    fun testRunContractArgument() {

        assertEquals(0, propertyRepository.count())

        contractService.createContract(DEFAULT_SPACE, DEFAULT_SELF, creator, FileUtils.getResourceAsString("/scripts/test.js"))

        assertTrue(Sets.newHashSet(propertyRepository.findAll()).containsAll(
                listOf(
                        Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, creator, "value1"), writeString("test1")),
                        Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, DEFAULT_SELF, "value3"), writeBoolean("false"))
                )
        ))

        val params = arrayOf("test", 22, true, BInteger(123))

        contractService.invokeContract(DEFAULT_FUNCTION_NAME, DEFAULT_SELF, caller, callee, ParameterUtils.writeParams(params))

        assertTrue(Sets.newHashSet(propertyRepository.findAll()).containsAll(
                listOf(
                        Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, caller, "value1"), writeString("test")),
                        Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, caller, "value2"), writeInteger("22")),
                        Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, DEFAULT_SELF, "value3"), writeBoolean("true")),
                        Property(PropertyId(DEFAULT_SPACE, DEFAULT_SELF, DEFAULT_SELF, "value4"), writeLong("124"))
                )
        ))

    }
}
