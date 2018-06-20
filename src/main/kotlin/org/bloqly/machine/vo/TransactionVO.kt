package org.bloqly.machine.vo

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils

@ValueObject
data class TransactionVO(
    val id: String,
    val space: String,
    val destination: String,
    val self: String?,
    val key: String?,
    val value: String,
    val transactionType: TransactionType,
    val referencedBlockId: String,
    val containingBlockId: String? = null,
    val timestamp: Long,
    val signature: String,
    val publicKey: String
) {

    fun toModel(): Transaction {

        val value = EncodingUtils.decodeFromString64(value)
        val signature = EncodingUtils.decodeFromString64(signature)

        val publicKeyBytes = EncodingUtils.decodeFromString16(publicKey)
        val publicKeyHash = CryptoUtils.digest(publicKeyBytes)
        val origin = EncodingUtils.encodeToString16(publicKeyHash)

        val transactionType = TransactionType.valueOf(transactionType.name)

        return Transaction(
            id = id,
            spaceId = space,
            origin = origin,
            destination = destination,
            self = self,
            key = key,
            value = value,
            transactionType = transactionType,
            referencedBlockId = referencedBlockId,
            containingBlockId = containingBlockId,
            timestamp = timestamp,
            signature = signature,
            publicKey = publicKey
        )
    }
}