package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.test.TestService
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

    @Before
    fun init() {

        testService.createBlockchain()

    }

    @After
    fun tearDown() {
        testService.cleanup()
    }

    @Test
    fun testExportGenesis() {
        //genesisService.exportGenesis();
    }

}