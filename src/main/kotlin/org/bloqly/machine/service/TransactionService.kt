package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.MAX_TRANSACTION_AGE
import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.model.ValueType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.TransactionRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(isolation = SERIALIZABLE)
class TransactionService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository
) {

    // TODO add blockchain config option - if adding smart contracts allowed
    @Transactional
    fun createTransaction(
        transactionRequest: TransactionRequest,
        referencedBlockHash: String
    ): Transaction {

        @Suppress("IMPLICIT_CAST_TO_ANY")
        val args: Array<Any> = transactionRequest.args
            .map {
                when (ValueType.valueOf(it.type)) {
                    ValueType.STRING -> it.value
                    ValueType.INT -> it.value.toInt()
                    ValueType.BIGINT -> BInteger(it.value)
                    ValueType.BOOLEAN -> it.value.toBoolean()
                }
            }.toTypedArray()

        val params = if (args.size > 1) {
            ParameterUtils.writeParams(args)
        } else {
            ParameterUtils.writeValue(args.first())
        }

        return createTransaction(
            space = transactionRequest.space,
            originId = transactionRequest.origin,
            passphrase = transactionRequest.passphrase,
            destinationId = transactionRequest.destination,
            self = transactionRequest.self,
            key = transactionRequest.key,
            value = params,
            transactionType = TransactionType.valueOf(transactionRequest.transactionType),
            referencedBlockHash = referencedBlockHash
        )
    }

    @Transactional
    fun createTransaction(
        space: String,
        originId: String,
        passphrase: String,
        destinationId: String,
        self: String,
        key: String? = null,
        value: ByteArray,
        transactionType: TransactionType,
        referencedBlockHash: String,
        timestamp: Long = Instant.now().toEpochMilli(),
        nonce: String = CryptoUtils.newNonce()
    ): Transaction {

        require(!transactionRepository.existsByNonce(nonce)) {
            "Transaction with nonce $nonce already exists"
        }

        val origin = accountRepository.findByAccountId(originId)!!

        val tx = Transaction(
            spaceId = space,
            origin = origin.accountId,
            destination = destinationId,
            self = self,
            key = key,
            value = value.encode16(),
            transactionType = transactionType,
            referencedBlockHash = referencedBlockHash,
            timestamp = timestamp,
            publicKey = origin.publicKey!!,
            nonce = nonce
        )

        val signature = CryptoUtils.sign(
            CryptoUtils.decrypt(origin.privateKeyEncoded, passphrase),
            CryptoUtils.hash(tx)
        )

        val hash = CryptoUtils.hash(signature)

        return transactionRepository.save(
            tx.copy(
                signature = signature.encode16(),
                hash = hash.encode16()
            )
        )
    }

    @Transactional(readOnly = true)
    fun getRecentTransactions(depth: Int): List<Transaction> {
        return spaceRepository.findAll()
            .flatMap { getPendingTransactionsBySpace(it.id, depth) }
    }

    @Transactional(readOnly = true)
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