package org.bloqly.machine.controller.admin

import org.bloqly.machine.component.GenesisService
import org.bloqly.machine.vo.GenesisEncoded
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/genesis")
class GenesisAdminController(
    private val genesisService: GenesisService,
    @Value("\${admin.port}") private val adminPort: Int
) {

    @GetMapping("/{spaceId}")
    fun exportGenesis(
        @PathVariable spaceId: String,
        request: HttpServletRequest
    ): ResponseEntity<GenesisEncoded> {

        require(request.localPort == adminPort)

        val genesis = genesisService.exportFirst(spaceId)

        return ResponseEntity(GenesisEncoded(genesis), HttpStatus.OK)
    }

    @PostMapping
    fun importGenesis(
        @RequestBody genesisEncoded: GenesisEncoded,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        require(request.localPort == adminPort)

        genesisService.importFirst(genesisEncoded.genesis)

        return ResponseEntity(HttpStatus.CREATED)
    }
}