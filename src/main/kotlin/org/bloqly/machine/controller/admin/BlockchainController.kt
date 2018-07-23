package org.bloqly.machine.controller.admin

import org.bloqly.machine.component.BlockchainService
import org.bloqly.machine.controller.admin.model.NewBlockchainRequest
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/blockchain")
class BlockchainController(
    private val blockchainService: BlockchainService
) {

    @PostMapping
    fun init(@RequestBody newBlockchainRequest: NewBlockchainRequest): ResponseEntity<Void> {

        blockchainService.createBlockchain(
            spaceId = newBlockchainRequest.space,
            baseDir = newBlockchainRequest.path,
            passphrase = newBlockchainRequest.passphrase
        )

        return ResponseEntity(HttpStatus.CREATED)
    }
}