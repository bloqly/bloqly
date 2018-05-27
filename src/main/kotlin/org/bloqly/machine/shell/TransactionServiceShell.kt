package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.Application
import org.bloqly.machine.component.SerializationService
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service
class TransactionServiceShell(

    private val blockRepository: BlockRepository,
    private val accountRepository: AccountRepository,
    private val transactionService: TransactionService,
    private val transactionRepository: TransactionRepository,
    private val serializationService: SerializationService,
    private val objectWriter: ObjectWriter) {

    fun createTransaction(originId: String, destinationId: String, amount: String): String {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(Application.DEFAULT_SPACE)

        val origin = accountRepository.findById(originId).orElseThrow()

        val destination = accountRepository.findById(destinationId).orElseThrow()

        val transaction = transactionService.newTransaction(

                space = Application.DEFAULT_SPACE,

                originId = origin.id,

                destinationId = destination.id,

                value = ParameterUtils.writeLong(amount),

                transactionType = TransactionType.CALL,

                referencedBlockId = lastBlock.id,

                timestamp = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond()
        )

        transactionRepository.save(transaction)

        val transactionVO = serializationService.transactionToVO(transaction)

        return "\n" + objectWriter.writeValueAsString(transactionVO)
    }


    fun count(): String {
        return transactionRepository.count().toString()
    }

    fun list(): String {

        val transactions = serializationService.transactionsToVO(
                transactionRepository.findAll().toList())

        return "\n" + objectWriter.writeValueAsString(transactions)
    }
}