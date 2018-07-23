package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.POWER_KEY
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.InvocationResult
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
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.math.BigInteger
import javax.transaction.Transactional

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val propertyRepository: PropertyRepository,
    private val env: Environment
) {

    companion object {
        private const val PASSPHRASE_PREFIX = "keys.passphrase_"

        private const val PASSPHRASE_SUFFIX_LENGTH = 8
    }

    fun getPassphrase(accountId: String): String {
        val key = PASSPHRASE_PREFIX + accountId.substring(0, PASSPHRASE_SUFFIX_LENGTH)
        return env.getRequiredProperty(key)
    }

    fun getProducerBySpace(space: Space, round: Long): Account {

        val validators = getValidatorsForSpace(space)

        val validatorIndex = round % validators.size

        return validators[validatorIndex.toInt()]
    }

    fun getActiveProducerBySpace(space: Space, round: Long): Account? {
        return getProducerBySpace(space, round).takeIf { it.hasKey() }
    }

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

    fun getValidatorsForSpace(space: Space): List<Account> {

        val powerProperties = propertyRepository.findBySpaceAndKey(space.id, POWER_KEY)
        val accountIds = powerProperties.map { it.id.target }

        return accountRepository
            .findAllByAccountIds(accountIds)
            .sortedBy { it.accountId }
    }

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

    fun getAccountByPublicKey(publicKey: String): Account {
        val publicKeyBytes = publicKey.decode16()

        val accountId = EncodingUtils.hashAndEncode16(publicKeyBytes)

        ensureAccount(accountId)

        val account = accountRepository.findByAccountId(accountId)!!

        return if (account.publicKey == null) {
            account.publicKey = publicKey
            accountRepository.save(account)
        } else {
            account
        }
    }

    fun ensureAccount(accountId: String) {
        if (!accountRepository.existsByAccountId(accountId)) {
            accountRepository.save(Account(accountId = accountId))
        }
    }

    fun ensureAccounts(result: InvocationResult) {
        if (!result.isOK()) {
            return
        }

        result.output.forEach { property ->
            ensureAccount(property.id.target)
        }
    }
}