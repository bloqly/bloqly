package org.bloqly.machine.vo.transaction

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.helper.CryptoHelper
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.vo.property.Value

@ValueObject
data class TransactionVO(
    val space: String,
    val destination: String,
    val self: String,
    val key: String?,
    val value: List<Value>,
    val transactionType: TransactionType,
    val referencedBlockHash: String,
    val timestamp: Long,
    val signature: String,
    val publicKey: String,
    val hash: String
) {

    fun toModel(): Transaction {
        val origin = CryptoHelper.publicKeyToAddress(publicKey)

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
            hash = hash
        )
    }
}