package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.component.PropertyContext
import org.bloqly.machine.component.TransactionProcessor
import org.bloqly.machine.lang.BLong
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.FileUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.property.Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
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
    private lateinit var transactionProcessor: TransactionProcessor

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    private val creator = "owner id"

    private val callee = "callee id"

    private val self = "test.js.self"

    @Test
    @Ignore
    fun testProcessTransactionPerformance() {

        val originId = testService.getRoot().accountId

        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val invokeContractTx = transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = originId,
            passphrase = passphrase(originId),
            destinationId = callee,
            self = DEFAULT_SELF,
            value = Value.ofs(1),
            transactionType = TransactionType.CALL,
            referencedBlockHash = lastBlock.hash
        )

        val propertyContext = PropertyContext(propertyService, contractService)

        val timeStart = System.currentTimeMillis()

        repeat(500) {
            val result = transactionProcessor.processTransaction(invokeContractTx, propertyContext)

            assertTrue(result.isOK())
        }

        val timeEnd = System.currentTimeMillis()

        System.out.println("TIME: " + (timeEnd - timeStart))
    }

    @Test
    fun testSetValue() {

        TimeUtils.testTickRound()
        createSetValueTx("value1")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        assertNull(propertyService.getPropertyValue(callee, "key1"))
        assertEquals("value1", getLastPropertyValue(callee, "key1"))

        TimeUtils.testTickRound()
        createSetValueTx("value2")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        assertNull(propertyService.getPropertyValue(callee, "key1"))
        assertEquals("value2", getLastPropertyValue(callee, "key1"))

        TimeUtils.testTickRound()
        createSetValueTx("value3")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        assertEquals("value1", getPropertyValue(callee, "key1"))
        assertEquals("value3", getLastPropertyValue(callee, "key1"))

        TimeUtils.testTickRound()
        createSetValueTx("value4")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        assertEquals("value2", getPropertyValue(callee, "key1"))
        assertEquals("value4", getLastPropertyValue(callee, "key1"))

        TimeUtils.testTickRound()
        createSetValueTx("value5")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        assertEquals("value3", getPropertyValue(callee, "key1"))
        assertEquals("value5", getLastPropertyValue(callee, "key1"))

        TimeUtils.testTickRound()
        createSetValueTx("value6")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        assertEquals("value4", getPropertyValue(callee, "key1"))
        assertEquals("value6", getLastPropertyValue(callee, "key1"))
    }

    private fun createSetValueTx(value: String): Transaction {

        val block = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val originId = testService.getRoot().accountId

        return transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = originId,
            passphrase = passphrase(originId),
            destinationId = callee,
            self = DEFAULT_SELF,
            key = "set",
            value = Value.of("key1", value),
            transactionType = TransactionType.CALL,
            referencedBlockHash = block.hash
        )
    }

    @Test
    fun testSetSignedValue() {

        TimeUtils.testTickRound()
        createSetSignedValueTx("value1")
        eventProcessorService.onProduceBlock()
        eventProcessorService.onGetVotes()

        val publicKey = testService.getUser().publicKey

        val key = "key1:$publicKey"

        assertNull(propertyService.getPropertyValue(callee, key))
        assertEquals("value1", getLastPropertyValue(callee, key))
    }

    private fun createSetSignedValueTx(value: String): Transaction {

        val block = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val originId = testService.getRoot().accountId

        val privateKey = testService.getUser().privateKey
        val publicKey = testService.getUser().publicKey

        val message = CryptoUtils.hash(value)

        val signature = CryptoUtils.sign(privateKey.decode16(), message)

        return transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = originId,
            passphrase = passphrase(originId),
            destinationId = callee,
            self = DEFAULT_SELF,
            key = "setSigned",
            value = Value.of("key1", value, signature.encode16(), publicKey),
            transactionType = TransactionType.CALL,
            referencedBlockHash = block.hash
        )
    }

    private fun getLastPropertyValue(target: String, key: String): Any? =
        blockProcessor.getLastPropertyValue(target, key)?.toValue()

    private fun getPropertyValue(target: String, key: String): Any? =
        propertyService.getPropertyValue(target, key)?.toValue()

    @Test
    fun testRunContractArgument() {

        val originId = testService.getValidator(0).accountId

        val propertyId1 = PropertyId(DEFAULT_SPACE, self, creator, "value1")
        val propertyId2 = PropertyId(DEFAULT_SPACE, self, self, "value3")

        val lastBlock = blockService.getLastBlockBySpace(DEFAULT_SPACE)

        val contractBody = FileUtils.getResourceAsString("/scripts/test.js")

        val createContractTx = transactionService.createTransaction(
            space = DEFAULT_SPACE,
            originId = originId,
            passphrase = passphrase(originId),
            destinationId = callee,
            self = self,
            value = Value.ofs(contractBody),
            transactionType = TransactionType.CREATE,
            referencedBlockHash = lastBlock.hash
        )

        val propertyContext = PropertyContext(propertyService, contractService)

        transactionProcessor.processTransaction(createContractTx, propertyContext)

        val contractProperties = propertyContext.properties

        assertTrue(Property(propertyId1, Value.of("test1")) in contractProperties)
        assertTrue(Property(propertyId2, Value.of(false)) in contractProperties)

        assertFalse(propertyRepository.existsById(propertyId1))
        assertFalse(propertyRepository.existsById(propertyId2))

        val params = Value.of("test", 22, true, BLong(123))

        TimeUtils.testTick()

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
                Value.of("test")
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, callee, "value2"),
                Value.of(22)
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, self, "value3"),
                Value.of(true)
            ) in propertiesAfter
        )
        assertTrue(
            Property(
                PropertyId(DEFAULT_SPACE, self, self, "value4"),
                Value.of(BLong("124"))
            ) in propertiesAfter
        )
    }
}
