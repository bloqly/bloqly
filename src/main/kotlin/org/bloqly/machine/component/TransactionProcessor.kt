package org.bloqly.machine.component

import org.bloqly.machine.Application.Companion.DEFAULT_KEY
import org.bloqly.machine.Application.Companion.INIT_KEY
import org.bloqly.machine.model.Contract
import org.bloqly.machine.model.InvocationContext
import org.bloqly.machine.model.InvocationResult
import org.bloqly.machine.model.InvocationResultType.ERROR
import org.bloqly.machine.model.InvocationResultType.SUCCESS
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType.CALL
import org.bloqly.machine.model.TransactionType.CREATE
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.ContractExecutorService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.decode16
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TransactionProcessor(
    private val contractExecutorService: ContractExecutorService,
    private val transactionService: TransactionService,
    private val blockService: BlockService
) {
    private val log = LoggerFactory.getLogger(TransactionProcessor::class.simpleName)

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
                body = String(tx.value.decode16())
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

        return contractExecutorService.invokeContract(propertyContext, invocationContext, tx.value.decode16())
    }

    @Transactional
    fun processTransaction(
        tx: Transaction,
        propertyContext: PropertyContext
    ): InvocationResult {

        try {

            // TODO check results are unique
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
        } catch (e: Exception) {
            log.error(e.message, e)
            return InvocationResult(ERROR)
        }
    }

    @Transactional
    fun isTransactionAcceptable(tx: Transaction): Boolean {

        if (tx.timestamp > TimeUtils.getCurrentTime()) {
            log.warn("Transaction is too old ${tx.toVO()}")
            return false
        }

        if (!blockService.existsByHash(tx.referencedBlockHash)) {
            log.warn("Not found referencedBlockHash for transaction ${tx.toVO()}")
            return false
        }

        if (!blockService.isActualTransaction(tx)) {
            log.warn("Transaction is not actual ${tx.toVO()}")
            return false
        }

        return true
    }
}