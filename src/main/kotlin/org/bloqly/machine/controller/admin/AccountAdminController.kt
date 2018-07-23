package org.bloqly.machine.controller.admin

import org.bloqly.machine.controller.admin.model.AccountRequest
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.util.decode16
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO only localhost
@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/accounts")
class AccountAdminController(
    private val accountService: AccountService
) {

    @PostMapping
    fun import(@RequestBody accountRequest: AccountRequest): ResponseEntity<Void> {

        accountService.importAccount(
            accountRequest.privateKey.decode16(),
            accountRequest.password
        )

        return ResponseEntity(HttpStatus.CREATED)
    }
}