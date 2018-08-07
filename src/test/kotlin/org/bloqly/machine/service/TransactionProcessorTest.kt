package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.PropertyContext
import org.bloqly.machine.component.TransactionProcessor
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.PropertyService
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.ParameterUtils.writeBoolean
import org.bloqly.machine.util.ParameterUtils.writeInteger
import org.bloqly.machine.util.ParameterUtils.writeLong
import org.bloqly.machine.util.ParameterUtils.writeString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class TransactionProcessorTest : BaseTest() {

    @Autowired
    private lateinit var contractService: ContractService

    @Autowired
    private lateinit var propertyService: PropertyService

    @Autowired
    private lateinit var transactionProcessor: TransactionProcessor

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    private val creator = "owner id"

    private val callee = "callee id"

    private val self = "test.js.self"

    @Test
    fun testRunContractArgument() {

        val originId = testService.getValidator(0).accountId

        val propertyId1 = PropertyId(DEFAULT_SPACE, self, creator, "value1")
        val propertyId2 = PropertyId(DEFAULT_SPACE, self, self, "value3")

        val lastBlock = blockService.getLastBlockForSpace(DEFAULT_SPACE)

        val contractBody = FileUtils.getResourceAsString("/scripts/test.js")

        val createContractTx = transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = originId,
            passphrase = passphrase(originId),
            destinationId = callee,
            self = self,
            value = contractBody.toByteArray(),
            transactionType = TransactionType.CREATE,
            referencedBlockHash = lastBlock.hash
        )

        val propertyContext = PropertyContext(propertyService, contractService)

        transactionProcessor.processTransaction(createContractTx, propertyContext)

        val contractProperties = propertyContext.properties

        assertTrue(Property(propertyId1, writeString("test1")) in contractProperties)
        assertTrue(Property(propertyId2, writeBoolean("false")) in contractProperties)

        assertFalse(propertyRepository.existsById(propertyId1))
        assertFalse(propertyRepository.existsById(propertyId2))

        val params = ParameterUtils.writeParams(arrayOf("test", 22, true, BInteger(123)))

        val invokeContractTx = transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = originId,
            passphrase = passphrase(originId),
            destinationId = callee,
            self = self,
            value = params,
            transactionType = TransactionType.CALL,
            referencedBlockHash = lastBlock.hash
        )

        transactionProcessor.processTransaction(invokeContractTx, propertyContext)

        val propertiesAfter = propertyContext.properties

        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, originId, "value1"),
                writeString("test")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, callee, "value2"),
                writeInteger("22")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, self, "value3"),
                writeBoolean("true")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, self, "value4"),
                writeLong("124")
            ) in propertiesAfter
        )
    }
}
