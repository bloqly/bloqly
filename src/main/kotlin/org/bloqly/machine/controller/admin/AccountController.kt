package org.bloqly.machine.controller.admin

import org.bloqly.machine.service.AccountService
import org.bloqly.machine.vo.AccountVO
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/accounts")
class AccountController(
    private val accountService: AccountService
) {

    @PutMapping
    fun import(@RequestBody account: AccountVO): ResponseEntity<Void> {

        accountService.importAccount(account.privateKey)

        return ResponseEntity(HttpStatus.CREATED)
    }
}