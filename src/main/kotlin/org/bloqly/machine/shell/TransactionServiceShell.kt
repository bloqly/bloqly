package org.bloqly.machine.shell

import com.fasterxml.jackson.databind.ObjectWriter
import org.bloqly.machine.Application
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.vo.TransactionList
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TransactionServiceShell(

    private val blockRepository: BlockRepository,
    private val accountRepository: AccountRepository,
    private val transactionService: TransactionService,
    private val transactionRepository: TransactionRepository,
    private val objectWriter: ObjectWriter
) {

    fun createTransaction(originId: String, destinationId: String, amount: String): String {

        val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(Application.DEFAULT_SPACE)

        val origin = accountRepository.findById(originId).orElseThrow()

        val transaction = transactionService.newTransaction(

            space = Application.DEFAULT_SPACE,

            originId = origin.id,

            destinationId = destinationId,

            value = ParameterUtils.writeLong(amount),

            transactionType = TransactionType.CALL,

            referencedBlockId = lastBlock.id,

            timestamp = Instant.now().toEpochMilli()
        )

        transactionRepository.save(transaction)

        val transactionVO = transaction.toVO()

        return "\n" + objectWriter.writeValueAsString(transactionVO)
    }

    fun count(): String {
        return transactionRepository.count().toString()
    }

    fun list(): String {

        val transactions = TransactionList.fromTransactions(
            transactionRepository.findAll().toList()
        )

        return "\n" + objectWriter.writeValueAsString(transactions)
    }
}