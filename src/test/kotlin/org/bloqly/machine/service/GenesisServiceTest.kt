package org.bloqly.machine.service

import org.bloqly.machine.Application
import org.bloqly.machine.component.GenesisService
import org.bloqly.machine.test.BaseTest
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.vo.Genesis
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
class GenesisServiceTest : BaseTest() {

    @Autowired
    private lateinit var genesisService: GenesisService

    @Test
    fun testExportGenesis() {
        val genesisString = genesisService.exportFirst(Application.DEFAULT_SPACE)

        val genesis = ObjectUtils.readValue(
            genesisString.decode16(),
            Genesis::class.java
        )

        assertEquals(1, genesis.transactions.size)
    }

    @Test
    fun testImportGenesis() {
        val genesisString = genesisService.exportFirst(Application.DEFAULT_SPACE)

        val genesis = ObjectUtils.readValue(
            genesisString.decode16(),
            Genesis::class.java
        )

        assertEquals(1, genesis.transactions.size)

        testService.cleanup()

        genesisService.importFirst(genesisString)

        testService.testPropertiesAreCreated()
        testService.testSpaceCreated()
        testService.testValidatorsPowerValues()
    }
}