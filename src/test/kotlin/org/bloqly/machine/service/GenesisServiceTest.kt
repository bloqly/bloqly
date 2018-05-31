package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.Assert.assertEquals
import org.bloqly.machine.Application
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.test.TestService
import org.bloqly.machine.vo.GenesisVO
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class GenesisServiceTest {

    @Autowired
    private lateinit var genesisService: GenesisService

    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var genesisString: String

    private lateinit var genesis: GenesisVO

    @Before
    fun init() {
        testService.createBlockchain()

        genesisString = genesisService.exportGenesis(DEFAULT_SPACE)

        println(genesisString)

        genesis = objectMapper.readValue(genesisString, GenesisVO::class.java)
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testExportGenesis() {
        assertEquals(1, genesis.transactions.size)
    }

    @Test
    fun testImportGenesis() {
        assertEquals(1, genesis.transactions.size)

        testService.cleanup()

        genesisService.importGenesis(genesisString)
    }
}