package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.MAX_TRANSACTION_AGE
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.util.encode64
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

        val origin = accountRepository.findByAccountId(originId)!!

        val tx = Transaction(
            spaceId = space,
            origin = origin.accountId,
            destination = destinationId,
            self = self,
            key = key,
            value = value.encode64(),
            transactionType = transactionType,
            referencedBlockHash = referencedBlockHash,
            timestamp = timestamp,
            publicKey = origin.publicKey!!
        )

        val signature = CryptoUtils.sign(
            origin.privateKeyBytes,
            CryptoUtils.hash(tx)
        )

        val hash = CryptoUtils.hash(signature)

        return transactionRepository.save(
            tx.copy(
                signature = signature.encode64(),
                hash = hash.encode16()
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

        val libHash = if (lastBlock.height > 0) {
            lastBlock.libHash
        } else {
            lastBlock.hash
        }

        val lib = blockRepository.findByHash(libHash)!!
        val minHeight = lib.height - depth

        return transactionRepository
            .findPendingTransactionsBySpaceId(spaceId, libHash, minTimestamp, minHeight)
    }
}