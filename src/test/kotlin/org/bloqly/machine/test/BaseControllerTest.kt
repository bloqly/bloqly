package org.bloqly.machine.test

import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

open class BaseControllerTest : BaseTest() {

    @Autowired
    protected lateinit var restTemplate: TestRestTemplate

    @LocalServerPort
    protected var port: Int = 0

    protected lateinit var node: Node

    protected val headers = HttpHeaders()

    @Before
    override fun setup() {
        super.setup()

        headers.contentType = MediaType.APPLICATION_JSON

        node = Node(NodeId("localhost", port), 0)
    }
}