package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.POWER_KEY
import org.bloqly.machine.component.PassphraseService
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.hashAndEncode16
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val propertyRepository: PropertyRepository,
    private val passphraseService: PassphraseService
) {

    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun getProducerBySpace(space: Space, round: Long): Account {

        val validators = getValidatorsForSpace(space)

        val validatorIndex = round % validators.size

        return validators[validatorIndex.toInt()]
    }

    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun getActiveProducerBySpace(space: Space, round: Long): Account? {
        return getProducerBySpace(space, round).takeIf { passphraseService.hasPassphrase(it.accountId) }
    }

    @Transactional(isolation = SERIALIZABLE)
    fun createAccount(passphrase: String): Account {

        return accountRepository.save(newAccount(passphrase))
    }

    fun newAccount(passphrase: String): Account {

        val privateKey = CryptoUtils.newPrivateKey()
        val publicKey = CryptoUtils.getPublicFor(privateKey)

        val account = Account(
            accountId = hashAndEncode16(publicKey),
            publicKey = publicKey.encode16()
        )

        account.privateKeyEncoded = CryptoUtils.encrypt(privateKey, passphrase)

        return account
    }

    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun getValidatorsForSpace(space: Space): List<Account> {

        val powerProperties = propertyRepository.findBySpaceAndKey(space.id, POWER_KEY)
        val accountIds = powerProperties.map { it.id.target }

        return accountRepository
            .findAllByAccountIds(accountIds)
            .sortedBy { it.accountId }
    }

    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun getAccountPower(space: String, accountId: String): BigInteger {

        val propertyKey = PropertyId(
            spaceId = space,
            self = DEFAULT_SELF,
            target = accountId,
            key = POWER_KEY
        )

        return propertyRepository.findById(propertyKey)
            .map { ParameterUtils.readValue(it.value) as BInteger }
            .map { it.value }
            .orElseThrow()
    }

    @Transactional(isolation = SERIALIZABLE)
    fun importAccount(privateKeyBytes: ByteArray?, passphrase: String): Account {

        val publicKeyBytes = CryptoUtils.getPublicFor(privateKeyBytes)
        val accountId = EncodingUtils.hashAndEncode16(publicKeyBytes)

        require(!accountRepository.existsByAccountId(accountId)) {
            "Could not import account: $accountId, account already exists."
        }

        val publicKey = publicKeyBytes.encode16()

        val account = Account(
            accountId = accountId,
            publicKey = publicKey
        )

        account.privateKeyEncoded = CryptoUtils.encrypt(privateKeyBytes, passphrase)

        return accountRepository.save(account)
    }

    @Transactional(isolation = SERIALIZABLE)
    fun ensureExistsAndGetByPublicKey(publicKey: String): Account {
        val publicKeyBytes = publicKey.decode16()

        val accountId = EncodingUtils.hashAndEncode16(publicKeyBytes)

        return accountRepository.findByAccountId(accountId)
            ?: accountRepository.save(
                Account(
                    accountId = accountId,
                    publicKey = publicKeyBytes.encode16()
                )
            )
    }
}