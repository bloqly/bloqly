package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION_NAME
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.TransactionProcessor
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.GenesisParameter
import org.bloqly.machine.model.GenesisParameters
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.test.TestService
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.ParameterUtils.writeBoolean
import org.bloqly.machine.util.ParameterUtils.writeInteger
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.ParameterUtils.writeString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class ContractExecutorServiceTest {

    @Autowired
    private lateinit var propertyService: PropertyService

    @Autowired
    private lateinit var contractExecutorService: ContractExecutorService

    @Autowired
    private lateinit var transactionProcessor: TransactionProcessor

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    @Autowired
    private lateinit var testService: TestService

    private val creator = "owner id"

    private val caller = "caller id"

    private val callee = "callee id"

    private val genesis = GenesisParameters(
        parameters = listOf(
            GenesisParameter(target = DEFAULT_SELF, key = "root", value = creator),
            GenesisParameter(target = creator, key = "value1", value = "test1"),
            GenesisParameter(target = DEFAULT_SELF, key = "value3", value = false)
        )
    )

    @Before
    fun setup() {
        testService.cleanup()
    }

    @Test
    fun testRunContractArgument() {

        assertEquals(0, propertyRepository.count())

        transactionProcessor.processCreateContract(
            DEFAULT_SPACE,
            DEFAULT_SELF,
            FileUtils.getResourceAsString("/scripts/test.js"),
            caller
        )

        propertyService.updateProperties(genesis)

        val propertiesBefore = propertyRepository.findAll()

        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, DEFAULT_SELF, creator, "value1"),
                writeString("test1")
            ) in propertiesBefore
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, DEFAULT_SELF, DEFAULT_SELF, "value3"),
                writeBoolean("false")
            ) in propertiesBefore
        )

        val params = arrayOf("test", 22, true, BInteger(123))

        contractExecutorService.invokeContract(
            DEFAULT_FUNCTION_NAME,
            DEFAULT_SELF,
            caller,
            callee,
            ParameterUtils.writeParams(params)
        )

        val propertiesAfter = propertyRepository.findAll()

        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, DEFAULT_SELF, caller, "value1"),
                writeString("test")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, DEFAULT_SELF, callee, "value2"),
                writeInteger("22")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, DEFAULT_SELF, DEFAULT_SELF, "value3"),
                writeBoolean("true")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, DEFAULT_SELF, DEFAULT_SELF, "value4"),
                writeLong("124")
            ) in propertiesAfter
        )
    }
}
