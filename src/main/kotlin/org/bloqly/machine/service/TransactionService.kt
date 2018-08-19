package org.bloqly.machine.service

import org.bloqly.machine.math.BInteger
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.model.ValueType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ParameterUtils
import org.bloqly.machine.util.TimeUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.TransactionRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
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
        // TODO do we need byte-array here?
        value: ByteArray,
        transactionType: TransactionType,
        referencedBlockHash: String,
        timestamp: Long = TimeUtils.getCurrentTime(),
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
            publicKey = origin.publicKey,
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun verifyAndSaveIfNotExists(tx: Transaction): Transaction {

        if (transactionRepository.existsByHash(tx.hash)) {
            return transactionRepository.getByHash(tx.hash)
        }

        // TODO where to check nonce?

        require(CryptoUtils.verifyTransaction(tx)) {
            "Could not verify transaction $tx"
        }

        return transactionRepository.save(tx)
    }

    @Transactional
    fun findByHash(hash: String): Transaction? =
        transactionRepository.findByHash(hash)

    @Transactional
    fun existsByHash(hash: String): Boolean =
        transactionRepository.existsByHash(hash)
}