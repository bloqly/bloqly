package org.bloqly.machine.controller.admin

import org.bloqly.machine.controller.admin.model.GenesisEncoded
import org.bloqly.machine.service.BlockService
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/genesis")
class GenesisController(
    private val blockService: BlockService
) {

    @GetMapping("/{spaceId}")
    fun exportGenesis(@PathVariable spaceId: String): ResponseEntity<GenesisEncoded> {

        val genesis = blockService.exportFirst(spaceId)

        return ResponseEntity(GenesisEncoded(genesis), HttpStatus.OK)
    }

    @PostMapping("/{spaceId}")
    fun importGenesis(@RequestBody genesisEncoded: GenesisEncoded): ResponseEntity<Void> {

        blockService.importFirst(genesisEncoded.genesis)

        return ResponseEntity(HttpStatus.CREATED)
    }
}