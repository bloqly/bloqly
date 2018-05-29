package org.bloqly.machine.service

import com.google.common.primitives.Bytes.concat
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.EncodingUtils.decodeFromString16
import org.bloqly.machine.util.EncodingUtils.encodeToString16
import org.springframework.stereotype.Service

@Service
class TransactionService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
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
            space = space,
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
            publicKey = origin.publicKey
        )
    }

    fun getNewTransactions(): List<Transaction> {
        // TODO: restrict by time
        return transactionRepository.findByContainingBlockIdIsNull()
    }
}