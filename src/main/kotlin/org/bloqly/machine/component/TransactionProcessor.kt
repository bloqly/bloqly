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
import org.bloqly.machine.service.AccountService
import org.bloqly.machine.service.ContractExecutorService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode64
import org.springframework.stereotype.Component
import javax.transaction.Transactional

@Component
@Transactional
class TransactionProcessor(
    private val accountService: AccountService,
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

        accountService.ensureAccount(tx.destination)

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

        accountService.ensureAccounts(result)

        propertyContext.updatePropertyValues(result.output)

        return result
    }
}