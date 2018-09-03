package org.bloqly.machine.controller.admin

import org.bloqly.machine.service.AccountService
import org.bloqly.machine.util.decode16
import org.bloqly.machine.vo.account.AccountImportRequest
import org.bloqly.machine.vo.account.AccountVO
import org.bloqly.machine.vo.account.PublicKeysImportRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/accounts")
class AccountAdminController(
    private val accountService: AccountService,
    @Value("\${admin.port}") private val adminPort: Int
) {

    @GetMapping("/validators")
    fun getMapping(@RequestParam("space") spaceId: String): List<AccountVO> {
        return accountService.findValidatorsForSpaceId(spaceId)
            ?.let { validators -> validators.map { it.toVO() } }
            ?: listOf()
    }

    @PostMapping
    fun import(
        @RequestBody accountImportRequest: AccountImportRequest,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        require(request.localPort == adminPort)

        accountService.importAccount(
            accountImportRequest.privateKey.decode16(),
            accountImportRequest.password
        )

        return ResponseEntity(CREATED)
    }

    @PostMapping("/publicKeys")
    fun importPublicKeys(
        @RequestBody publicKeysImportRequest: PublicKeysImportRequest,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        require(request.localPort == adminPort)

        publicKeysImportRequest.publicKeys
            .forEach { accountService.importAccountPublicKey(it) }

        return ResponseEntity(CREATED)
    }
}