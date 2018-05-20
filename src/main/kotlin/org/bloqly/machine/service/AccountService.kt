package org.bloqly.machine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.POWER_KEY
import org.bloqly.machine.component.CryptoService
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.encodeToString16
import org.bloqly.machine.util.ParameterUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.math.BigInteger
import javax.transaction.Transactional

@Service
@Transactional
class AccountService(

    private val cryptoService: CryptoService,
    private val accountRepository: AccountRepository,
    private val propertyRepository: PropertyRepository,
    private val blockRepository: BlockRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${validators:}") private val validators: Array<String>

) {

    fun readAccounts(baseDir: String): List<Account> {

        val accountsString = File("$baseDir/accounts.yaml").readText()

        val accountsListType = objectMapper.typeFactory.constructCollectionType(
                List::class.java, Account::class.java)

        return objectMapper.readValue(accountsString, accountsListType)
    }

    fun createAccount(): Account {

        return accountRepository.save(newAccount())
    }

    fun newAccount(): Account {

        val privateKey = cryptoService.generatePrivateKey()
        val publicKey = cryptoService.getPublicFor(privateKey)
        val publicKeyHash = cryptoService.digest(publicKey)
        val accountId = EncodingUtils.encodeToString16(publicKeyHash)

        return Account(
                id = accountId,
                publicKey = encodeToString16(publicKey),
                privateKey = encodeToString16(privateKey)
        )
    }

    fun getValidatorsForSpace(space: String): List<Account> {

        val powerProperties = propertyRepository.findBySpaceAndKey(space, POWER_KEY)
        val accountIds = powerProperties.map { it.id.target }

        return accountRepository.findAllById(accountIds)
                .filter { validators.isEmpty() || validators.contains(it.id) }
    }

    fun getAccountPower(space: String, accountId: String): BigInteger {

        val propertyKey = PropertyId(
                space = space,
                self = DEFAULT_SELF,
                target = accountId,
                key = POWER_KEY
        )

        return propertyRepository.findById(propertyKey)
                .map { ParameterUtils.readValue(it.value) as BInteger }
                .map { it.value }
                .orElseThrow()
    }

    fun importAccount(publicKey: String, privateKey: String?): Account {

        val publicKeyBytes = EncodingUtils.decodeFromString16(publicKey)
        val publicKeyHash = cryptoService.digest(publicKeyBytes)
        val accountId = EncodingUtils.encodeToString16(publicKeyHash)

        val account = Account(
                id = accountId,
                publicKey = publicKey,
                privateKey = privateKey
        )

        return accountRepository.save(account)
    }

    fun getRoot(space: String): Account {

        val firstBlock = blockRepository.findGenesisBlockBySpace(space)

        return accountRepository.findById(firstBlock.proposerId).orElseThrow()
    }
}