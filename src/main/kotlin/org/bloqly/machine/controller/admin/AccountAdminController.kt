package org.bloqly.machine.controller.admin

import org.bloqly.machine.controller.admin.model.AccountRequest
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.util.decode16
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
@RequestMapping("/api/v1/admin/accounts")
class AccountAdminController(
    private val accountService: AccountService,
    @Value("\${admin.port}") private val adminPort: Int
) {

    @PostMapping
    fun import(
        @RequestBody accountRequest: AccountRequest,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        require(request.localPort == adminPort)

        accountService.importAccount(
            accountRequest.privateKey.decode16(),
            accountRequest.password
        )

        return ResponseEntity(HttpStatus.CREATED)
    }
}