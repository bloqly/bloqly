package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.POWER_KEY
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.encodeToString16
import org.bloqly.machine.util.EncodingUtils.hashAndEncode16
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.TimeUtils
import org.springframework.stereotype.Service
import java.math.BigInteger
import javax.transaction.Transactional

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val propertyRepository: PropertyRepository
) {
    // TODO this method can theoretically return null
    fun getActiveValidator(space: String): Account {

        val round = TimeUtils.getCurrentRound()

        val validators = getValidatorsForSpace(space)

        val validatorIndex = round % validators.size

        return validators[validatorIndex.toInt()]
    }

    fun createAccount(): Account {

        return accountRepository.save(newAccount())
    }

    fun newAccount(): Account {

        val privateKey = CryptoUtils.generatePrivateKey()
        val publicKey = CryptoUtils.getPublicFor(privateKey)

        return Account(
            id = hashAndEncode16(publicKey),
            publicKey = encodeToString16(publicKey),
            privateKey = encodeToString16(privateKey)
        )
    }

    fun getValidatorsForSpace(space: String): List<Account> {

        val powerProperties = propertyRepository.findBySpaceAndKey(space, POWER_KEY)
        val accountIds = powerProperties.map { it.id.target }

        return accountRepository
            .findAllById(accountIds)
            .sortedBy { it.id }
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

    fun importAccount(privateKey: String): Account {

        val privateKeyBytes = EncodingUtils.decodeFromString16(privateKey)
        val publicKeyBytes = CryptoUtils.getPublicFor(privateKeyBytes)
        val publicKeyHash = CryptoUtils.digest(publicKeyBytes)
        val accountId = EncodingUtils.encodeToString16(publicKeyHash)

        require(!accountRepository.existsById(accountId)) {
            "Could not import account: $accountId, account already exists."
        }

        val publicKey = EncodingUtils.encodeToString16(publicKeyBytes)

        return accountRepository.save(
            Account(
                id = accountId,
                publicKey = publicKey,
                privateKey = privateKey
            )
        )
    }
}