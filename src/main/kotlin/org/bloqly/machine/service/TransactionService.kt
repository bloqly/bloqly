package org.bloqly.machine.service

import com.google.common.primitives.Bytes.concat
import org.bloqly.machine.Application.Companion.MAX_TRANSACTION_AGE
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TransactionService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository
) {

    fun createTransaction(
        space: String,
        originId: String,
        destinationId: String,
        self: String,
        key: String? = null,
        value: ByteArray,
        transactionType: TransactionType,
        referencedBlockHash: String,
        timestamp: Long = Instant.now().toEpochMilli()
    ): Transaction {

        val dataToSign = concat(
            space.toByteArray(),
            originId.toByteArray(),
            destinationId.toByteArray(),
            value,
            referencedBlockHash.toByteArray(),
            transactionType.name.toByteArray(),
            EncodingUtils.longToBytes(timestamp)
        )

        val origin = accountRepository.findById(originId).orElseThrow()

        val privateKey = origin.privateKey.decode16()

        val signature = CryptoUtils.sign(
            privateKey,
            CryptoUtils.hash(dataToSign)
        )

        val txHash = CryptoUtils.hash(signature)

        return transactionRepository.save(
            Transaction(
                spaceId = space,
                origin = origin.id,
                destination = destinationId,
                self = self,
                key = key,
                value = value,
                transactionType = transactionType,
                referencedBlockHash = referencedBlockHash,
                timestamp = timestamp,
                signature = signature,
                publicKey = origin.publicKey!!,
                hash = txHash.encode16()
            )
        )
    }

    fun getRecentTransactions(depth: Int): List<Transaction> {
        return spaceRepository.findAll()
            .flatMap { getPendingTransactionsBySpace(it.id, depth) }
    }

    fun getPendingTransactionsBySpace(spaceId: String, depth: Int): List<Transaction> {
        val lastBlock = blockRepository.getLastBlock(spaceId)
        val minTimestamp = TimeUtils.getCurrentTime() - MAX_TRANSACTION_AGE
        val minHeight = lastBlock.height - depth
        return transactionRepository
            .findPendingTransactionsBySpaceId(spaceId, lastBlock.hash, minTimestamp, minHeight)
    }
}