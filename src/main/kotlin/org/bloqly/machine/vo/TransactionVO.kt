package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.decode64
import org.bloqly.machine.util.encode16

@ValueObject
data class TransactionVO(
    val id: String,
    val space: String,
    val destination: String,
    val self: String,
    val key: String?,
    val value: String,
    val transactionType: TransactionType,
    val referencedBlockId: String,
    val timestamp: Long,
    val signature: String,
    val publicKey: String
) {

    fun toModel(): Transaction {
        // TODO after introducing Schnorr change it to recover from signature if possible
        val publicKeyBytes = publicKey.decode16()
        val publicKeyHash = CryptoUtils.hash(publicKeyBytes)
        val origin = publicKeyHash.encode16()

        val transactionType = TransactionType.valueOf(transactionType.name)

        return Transaction(
            id = id,
            spaceId = space,
            origin = origin,
            destination = destination,
            self = self,
            key = key,
            value = value.decode64(),
            transactionType = transactionType,
            referencedBlockId = referencedBlockId,
            timestamp = timestamp,
            signature = signature.decode64(),
            publicKey = publicKey
        )
    }
}