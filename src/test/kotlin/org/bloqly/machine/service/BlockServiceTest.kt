package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.Genesis
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class BlockServiceTest {

    @Autowired
    private lateinit var blockService: BlockService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Before
    fun init() {
        testService.cleanup()
        testService.createBlockchain()
    }

    @Test
    fun testExportGenesis() {
        val genesisString = blockService.exportFirst(Application.DEFAULT_SPACE)

        val genesis = objectMapper.readValue(genesisString, Genesis::class.java)

        assertEquals(1, genesis.transactions.size)
    }

    @Test
    fun testImportGenesis() {
        val genesisString = blockService.exportFirst(Application.DEFAULT_SPACE)

        val genesis = objectMapper.readValue(genesisString, Genesis::class.java)

        assertEquals(1, genesis.transactions.size)

        testService.cleanup()

        blockService.importFirst(genesisString)

        testService.testPropertiesAreCreated()
        testService.testSpaceCreated()
        testService.testValidatorsInitialized()
        testService.testValidatorsPowerValues()
    }
}