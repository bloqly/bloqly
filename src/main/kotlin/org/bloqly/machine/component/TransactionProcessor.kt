package org.bloqly.machine.component

import org.apache.commons.lang3.StringUtils
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.service.ContractService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
@Transactional
class TransactionProcessor(
    private val contractRepository: ContractRepository,
    private val accountRepository: AccountRepository,
    private val contractService: ContractService
) {

    fun createContract(
        spaceId: String,
        self: String,
        body: String,
        owner: String
    ) {

        require(StringUtils.isNotEmpty(body)) {
            "Contract body can not be empty"
        }

        val contract = Contract(self, spaceId, owner, body)

        contractRepository.save(contract)

        contractService.invokeContract("init", self, owner, self, byteArrayOf())
    }

    private fun createContract(tx: Transaction) {

        return createContract(tx.spaceId, tx.self, String(tx.value), tx.origin)
    }

    fun call(transaction: Transaction) {

        // contract id
        val self = transaction.self

        // contract function name
        val key = transaction.key

        // contract arguments
        val arg = transaction.value
        val caller = transaction.origin
        val callee = transaction.destination

        contractService.invokeContract(key, self, caller, callee, arg)
    }

    fun processTransaction(tx: Transaction) {

        accountRepository.insertAccountIdIfNotExists(tx.origin)
        accountRepository.insertAccountIdIfNotExists(tx.destination)
        accountRepository.insertAccountIdIfNotExists(tx.self)

        val origin = accountRepository.findById(tx.origin).orElseThrow()

        val publicKey = tx.publicKey.decode16()

        val publicKeyHash = CryptoUtils.digest(publicKey).encode16()

        if (origin.publicKey == null && publicKeyHash == origin.id) {
            accountRepository.save(origin.copy(publicKey = tx.publicKey))
        }

        when (tx.transactionType) {

            TransactionType.CREATE -> createContract(tx)

            TransactionType.CALL -> call(tx)

            else -> {
                // do nothing
            }
        }
    }
}