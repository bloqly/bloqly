package org.bloqly.machine.test

import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

abstract class BaseBlockchainTest {

    @Autowired
    protected lateinit var testService: TestService

    @Before
    fun init() {
        testService.createBlockchain()
    }

    @After
    fun tearDown() {
        testService.cleanup()
    }
}