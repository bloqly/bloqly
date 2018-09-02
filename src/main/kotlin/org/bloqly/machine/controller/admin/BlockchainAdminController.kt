package org.bloqly.machine.controller.admin

import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.vo.genesis.NewBlockchainRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/blockchain")
class BlockchainAdminController(
    private val blockchainService: BlockchainService,
    @Value("\${admin.port}") private val adminPort: Int
) {

    @PostMapping
    fun init(
        @RequestBody newBlockchainRequest: NewBlockchainRequest,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        require(request.localPort == adminPort)

        blockchainService.createBlockchain(
            spaceId = newBlockchainRequest.space,
            baseDir = newBlockchainRequest.path,
            passphrase = newBlockchainRequest.passphrase
        )

        return ResponseEntity(HttpStatus.CREATED)
    }
}