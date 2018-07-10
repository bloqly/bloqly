package org.bloqly.machine.component

import org.apache.commons.lang3.StringUtils
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.InvocationResultType
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType.CALL
import org.bloqly.machine.model.TransactionType.CREATE
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.ContractRepository
import org.bloqly.machine.repository.TransactionRepository
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
    private val contractService: ContractService,
    private val transactionRepository: TransactionRepository
) {

    fun processCreateContract(
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

    private fun processCreateContract(tx: Transaction) {
        return processCreateContract(tx.spaceId, tx.self, String(tx.value), tx.origin)
    }

    fun processCall(tx: Transaction) {

        val invocationResult = contractService.invokeContract(tx.key, tx.self, tx.origin, tx.destination, tx.value)

        if (invocationResult.invocationResultType == InvocationResultType.SUCCESS) {
            //transactionRepository.updateOutput(tx.id, invocationResult.result)
        }
    }

    fun processTransaction(tx: Transaction) {

        accountRepository.insertAccountIdIfNotExists(tx.origin)
        accountRepository.insertAccountIdIfNotExists(tx.destination)
        accountRepository.insertAccountIdIfNotExists(tx.self)

        val origin = accountRepository.findById(tx.origin).orElseThrow()

        val publicKey = tx.publicKey.decode16()

        val address = CryptoUtils.digest(publicKey).encode16()

        if (origin.publicKey == null && address == origin.id) {
            accountRepository.save(origin.copy(publicKey = tx.publicKey))
        }

        when (tx.transactionType) {

            CREATE ->
                processCreateContract(tx)

            CALL ->
                processCall(tx)

            else -> {
                // do nothing
            }
        }
    }
}