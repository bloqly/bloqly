package org.bloqly.machine.controller.admin

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import org.bloqly.machine.service.AccountService
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

@Api(
    value = "/api/v1/admin/accounts",
    authorizations = [
        Authorization(value = "Basic", scopes = [])
    ]
)
@Profile("server")
@RestController
@RequestMapping("/api/v1/admin/accounts")
class AccountAdminController(
    private val accountService: AccountService,
    @Value("\${admin.port}") private val adminPort: Int
) {

    @ApiOperation(
        value = "Returns list of current validators",
        response = AccountVO::class,
        responseContainer = "List",
        nickname = "getValidators"
    )
    @GetMapping("/validators")
    fun getValidators(
        @ApiParam(value = "Space id, default value is 'main'", example = "main")
        @RequestParam("space") spaceId: String
    ): List<AccountVO> {
        return accountService.findValidatorsForSpaceId(spaceId)
            ?.let { validators -> validators.map { it.toVO() } }
            ?: listOf()
    }

    @ApiOperation(
        value = "Imports account",
        nickname = "importAccount"
    )
    @PostMapping
    fun importAccount(
        @RequestBody accountImportRequest: AccountImportRequest,
        request: HttpServletRequest
    ): ResponseEntity<Void> {

        require(request.localPort == adminPort)

        accountService.importAccount(
            accountImportRequest.publicKey,
            accountImportRequest.privateKeyEncrypted
        )

        return ResponseEntity(CREATED)
    }

    @ApiOperation(
        value = "Imports account public keys",
        nickname = "importPublicKeys"
    )
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

    @ApiOperation(
        value = "Creates new account",
        nickname = "createAccount"
    )
    @PostMapping("/new")
    fun createAccount(@RequestParam("passphrase") passphrase: String): AccountVO {
        return accountService.createAccount(passphrase).toFullVO()
    }
}