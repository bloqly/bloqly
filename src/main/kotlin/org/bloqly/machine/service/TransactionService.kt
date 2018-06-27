package org.bloqly.machine.service

import com.google.common.primitives.Bytes.concat
import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.Application.Companion.MAX_TRANSACTION_AGE
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.bloqly.machine.util.EncodingUtils.encodeToString16
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TransactionService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val blockRepository: BlockRepository
) {

    fun newTransaction(
        space: String,
        originId: String,
        destinationId: String,
        self: String? = null,
        key: String? = null,
        value: ByteArray,
        transactionType: TransactionType,
        referencedBlockId: String,
        containingBlockId: String? = null,
        timestamp: Long
    ): Transaction {

        val dataToSign = concat(
            space.toByteArray(),
            originId.toByteArray(),
            destinationId.toByteArray(),
            value,
            referencedBlockId.toByteArray(),
            transactionType.name.toByteArray(),
            EncodingUtils.longToBytes(timestamp)
        )

        val origin = accountRepository.findById(originId).orElseThrow()

        val privateKey = decodeFromString16(origin.privateKey)

        val signature = CryptoUtils.sign(
            privateKey,
            CryptoUtils.digest(dataToSign)
        )

        val txHash = CryptoUtils.digest(signature)
        val transactionId = encodeToString16(txHash)

        return Transaction(
            id = transactionId,
            spaceId = space,
            origin = origin.id,
            destination = destinationId,
            self = self,
            key = key,
            value = value,
            transactionType = transactionType,
            referencedBlockId = referencedBlockId,
            containingBlockId = containingBlockId,
            timestamp = timestamp,
            signature = signature,
            publicKey = origin.publicKey!!
        )
    }

    fun isActual(transaction: Transaction): Boolean {

        val referencedBlock = blockRepository.findById(transaction.referencedBlockId).orElseThrow()
        val lastBlock = blockRepository.getLastBlock(transaction.spaceId)

        return lastBlock.height - referencedBlock.height <= MAX_REFERENCED_BLOCK_DEPTH
    }

    fun getPendingTransactions(): List<Transaction> {
        val minTimestamp = Instant.now().toEpochMilli() - MAX_TRANSACTION_AGE
        return transactionRepository
            .findPendingTransactions(minTimestamp)
            .filter { isActual(it) }
    }

    fun getPendingTransactionsBySpace(spaceId: String): List<Transaction> {
        val minTimestamp = Instant.now().toEpochMilli() - MAX_TRANSACTION_AGE
        return transactionRepository
            .findPendingTransactionsBySpaceId(spaceId, minTimestamp)
            .filter { isActual(it) }
    }
}