package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.POWER_KEY
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Account
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils.hashAndEncode16
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Service
import java.math.BigInteger
import javax.transaction.Transactional

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
    private val propertyRepository: PropertyRepository
) {

    fun getProducerBySpace(space: Space, round: Long): Account {

        val validators = getValidatorsForSpace(space)

        val validatorIndex = round % validators.size

        return validators[validatorIndex.toInt()]
    }

    fun getActiveProducerBySpace(space: Space, round: Long): Account? {
        return getProducerBySpace(space, round).takeIf { it.hasKey() }
    }

    fun createAccount(): Account {

        return accountRepository.save(newAccount())
    }

    fun newAccount(): Account {

        val privateKey = CryptoUtils.newPrivateKey()
        val publicKey = CryptoUtils.getPublicFor(privateKey)

        return Account(
            id = hashAndEncode16(publicKey),
            publicKey = publicKey.encode16(),
            privateKey = privateKey.encode16()
        )
    }

    fun getValidatorsForSpace(space: Space): List<Account> {

        val powerProperties = propertyRepository.findBySpaceAndKey(space.id, POWER_KEY)
        val accountIds = powerProperties.map { it.id.target }

        return accountRepository
            .findAllById(accountIds)
            .sortedBy { it.id }
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

    fun importAccount(privateKey: String): Account {

        val privateKeyBytes = privateKey.decode16()
        val publicKeyBytes = CryptoUtils.getPublicFor(privateKeyBytes)
        val publicKeyHash = CryptoUtils.hash(publicKeyBytes)
        val accountId = publicKeyHash.encode16()

        require(!accountRepository.existsById(accountId)) {
            "Could not import account: $accountId, account already exists."
        }

        val publicKey = publicKeyBytes.encode16()

        return accountRepository.save(
            Account(
                id = accountId,
                publicKey = publicKey,
                privateKey = privateKey
            )
        )
    }
}