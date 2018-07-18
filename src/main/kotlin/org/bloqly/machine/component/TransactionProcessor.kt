package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_KEY
import org.bloqly.machine.Application.Companion.INIT_KEY
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.InvocationContext
import org.bloqly.machine.model.InvocationResult
import org.bloqly.machine.model.InvocationResultType.SUCCESS
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType.CALL
import org.bloqly.machine.model.TransactionType.CREATE
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.service.ContractExecutorService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.decode64
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
@Transactional
class TransactionProcessor(
    private val accountRepository: AccountRepository,
    private val contractExecutorService: ContractExecutorService
) {

    private fun processCreateContract(
        tx: Transaction,
        propertyContext: PropertyContext
    ): InvocationResult {

        val invocationContext = InvocationContext(
            space = tx.spaceId,
            owner = tx.origin,
            self = tx.self,
            key = INIT_KEY,
            caller = tx.origin,
            callee = tx.destination
        )

        propertyContext.saveContract(
            Contract(
                id = tx.self,
                space = tx.spaceId,
                owner = tx.origin,
                body = String(tx.value.decode64())
            )
        )

        return contractExecutorService.invokeContract(propertyContext, invocationContext, byteArrayOf())
    }

    private fun processCall(
        tx: Transaction,
        propertyContext: PropertyContext
    ): InvocationResult {

        require(CryptoUtils.verifyTransaction(tx)) {
            "Could not verify transaction ${tx.toVO()}"
        }

        val invocationContext = InvocationContext(
            space = tx.spaceId,
            self = tx.self,
            key = tx.key ?: DEFAULT_KEY,
            caller = tx.origin,
            callee = tx.destination
        )

        return contractExecutorService.invokeContract(propertyContext, invocationContext, tx.value.decode64())
    }

    fun processTransaction(
        tx: Transaction,
        propertyContext: PropertyContext
    ): InvocationResult {

        accountRepository.insertAccountIdIfNotExists(tx.origin)
        accountRepository.insertAccountIdIfNotExists(tx.destination)
        accountRepository.insertAccountIdIfNotExists(tx.self)

        val origin = accountRepository.findById(tx.origin).orElseThrow()

        val publicKey = tx.publicKey.decode16()

        val address = CryptoUtils.hash(publicKey).encode16()

        if (origin.publicKey == null && address == origin.id) {
            accountRepository.save(origin.copy(publicKey = tx.publicKey))
        }

        val result = when (tx.transactionType) {

            CREATE -> {
                processCreateContract(tx, propertyContext)
            }

            CALL -> {
                processCall(tx, propertyContext)
            }

            else -> {
                // do nothing
                InvocationResult(SUCCESS)
            }
        }

        propertyContext.updatePropertyValues(result.output)

        return result
    }
}