package org.bloqly.machine.service

import org.bitcoinj.core.ECKey
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.POWER_KEY
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.crypto.CryptoUtils
import org.bloqly.machine.crypto.toAddress
import org.bloqly.machine.lang.BLong
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.util.fromHex
import org.bloqly.machine.util.toHex
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val propertyRepository: PropertyRepository,
    private val passphraseService: PassphraseService,
    private val spaceRepository: SpaceRepository
) {

    fun isProducerValidForRound(spaceId: String, producerId: String, round: Long): Boolean {
        val space = spaceRepository.findById(spaceId).orElseThrow()

        val activeValidator = getProducerBySpace(space, round)

        return activeValidator != null && activeValidator.accountId == producerId
    }

    @Transactional(readOnly = true)
    fun getProducerBySpace(space: Space, round: Long): Account? {

        val validators = findValidatorsForSpace(space)

        return validators?.let {
            val validatorIndex = round % validators.size

            validators[validatorIndex.toInt()]
        }
    }

    @Transactional(readOnly = true)
    fun getActiveProducerBySpace(space: Space, round: Long): Account? {
        return getProducerBySpace(space, round)
            .takeIf { it != null && passphraseService.hasPassphrase(it.accountId) }
    }

    @Transactional
    fun createAccount(passphrase: String): Account {

        return accountRepository.save(newAccount(passphrase))
    }

    fun newAccount(passphrase: String): Account {

        val key = ECKey()

        val privateKey = key.privKeyBytes
        val publicKey = CryptoUtils.getPublicFor(privateKey)

        val account = Account(
            accountId = publicKey.toAddress(),
            publicKey = publicKey.toHex()
        )

        account.privateKeyEncoded = CryptoUtils.encrypt(privateKey, passphrase)

        return account
    }

    @Transactional(readOnly = true)
    fun findValidatorsForSpaceId(spaceId: String): List<Account>? {
        val space = spaceRepository.findById(spaceId).orElseThrow()

        return findValidatorsForSpace(space)
    }

    @Transactional(readOnly = true)
    fun findValidatorsForSpace(space: Space): List<Account>? {
        val powerProperties = propertyRepository.findBySpaceAndKey(space.id, POWER_KEY)
        val validatorsCount = propertyRepository.getValidatorsCountSpaceId(space.id)

        val accountIds = powerProperties.map { it.id.target }

        val validators = accountRepository.findAllByAccountIds(accountIds)

        return validators
            .takeIf { validators.size == validatorsCount }
            ?.sortedBy { it.accountId }
    }

    @Transactional(readOnly = true)
    fun getAccountPower(space: String, accountId: String): BigInteger {

        val propertyKey = PropertyId(
            spaceId = space,
            self = DEFAULT_SELF,
            target = accountId,
            key = POWER_KEY
        )

        return propertyRepository.findById(propertyKey)
            .map { it.toValue() as BLong }
            .map { it.value }
            .orElseThrow()
    }

    @Transactional
    fun importAccountPublicKey(publicKey: String) {

        val accountId = publicKey.toAddress()

        if (!accountRepository.existsByAccountId(accountId)) {
            accountRepository.save(
                Account(
                    accountId = accountId,
                    publicKey = publicKey
                )
            )
        }
    }

    @Transactional
    fun importAccount(privateKeyBytes: ByteArray?, passphrase: String) {

        val publicKeyBytes = CryptoUtils.getPublicFor(privateKeyBytes)
        val accountId = publicKeyBytes.toAddress()

        require(!accountRepository.existsByAccountId(accountId)) {
            "Account $accountId already exists"
        }

        accountRepository.save(
            Account(
                accountId = accountId,
                publicKey = publicKeyBytes.toHex(),
                privateKeyEncoded = CryptoUtils.encrypt(privateKeyBytes, passphrase)
            )
        )
    }

    @Transactional
    fun ensureExistsAndGetByPublicKey(publicKey: String): Account {
        val publicKeyBytes = publicKey.fromHex()

        val accountId = publicKeyBytes.toAddress()

        return accountRepository.findByAccountId(accountId)
            ?: accountRepository.save(
                Account(
                    accountId = accountId,
                    publicKey = publicKeyBytes.toHex()
                )
            )
    }
}