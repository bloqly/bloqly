package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16

@ValueObject
data class TransactionVO(
    val space: String,
    val destination: String,
    val self: String,
    val key: String?,
    val value: String,
    val transactionType: TransactionType,
    val referencedBlockHash: String,
    val timestamp: Long,
    val signature: String,
    val publicKey: String,
    val hash: String,
    val nonce: String
) {

    fun toModel(): Transaction {
        val publicKeyBytes = publicKey.decode16()
        val publicKeyHash = CryptoUtils.hash(publicKeyBytes)
        val origin = publicKeyHash.encode16()

        val transactionType = TransactionType.valueOf(transactionType.name)

        return Transaction(
            spaceId = space,
            origin = origin,
            destination = destination,
            self = self,
            key = key,
            value = value,
            transactionType = transactionType,
            referencedBlockHash = referencedBlockHash,
            timestamp = timestamp,
            signature = signature,
            publicKey = publicKey,
            hash = hash,
            nonce = nonce
        )
    }
}