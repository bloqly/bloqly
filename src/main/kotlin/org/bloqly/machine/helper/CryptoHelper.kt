package org.bloqly.machine.helper

import com.google.common.primitives.Bytes
import org.bloqly.machine.crypto.CryptoUtils
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.util.fromHex
import org.bloqly.machine.util.toHex
import org.bloqly.machine.vo.property.PropertyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

object CryptoHelper {

    private val log: Logger = LoggerFactory.getLogger(CryptoHelper::class.simpleName)

    fun hash(tx: Transaction): ByteArray {
        val valueString = ObjectUtils.writeValueAsString(tx.value)

        return CryptoUtils.hash(
            Bytes.concat(
                tx.spaceId.toByteArray(),
                tx.origin.toByteArray(),
                tx.destination.toByteArray(),
                tx.self.toByteArray(),
                tx.key?.toByteArray() ?: byteArrayOf(),
                valueString.toByteArray(),
                tx.referencedBlockHash.fromHex(),
                tx.transactionType.name.toByteArray(),
                EncodingUtils.longToBytes(tx.timestamp)
            )
        )
    }

    fun verifyTransaction(tx: Transaction): Boolean {
        try {
            val signature = tx.signature.fromHex()

            val txHash = CryptoUtils.hash(signature).toHex()

            if (txHash != tx.hash) {
                return false
            }

            val txDataHash = hash(tx)

            return CryptoUtils.verify(
                message = txDataHash,
                signature = signature,
                publicKey = tx.publicKey.fromHex()
            )
        } catch (e: Exception) {
            log.warn(e.message)
            log.error(e.message, e)
            return false
        }
    }

    fun hash(vote: Vote): ByteArray {
        return CryptoUtils.hash(
            Bytes.concat(
                vote.publicKey.fromHex(),
                vote.blockHash.fromHex(),
                EncodingUtils.longToBytes(vote.height),
                vote.spaceId.toByteArray(),
                EncodingUtils.longToBytes(vote.timestamp)
            )
        )
    }

    fun hash(block: Block, txHash: ByteArray = ByteArray(0), validatorTxHash: ByteArray): ByteArray {
        return CryptoUtils.hash(
            Bytes.concat(
                block.spaceId.toByteArray(),
                EncodingUtils.longToBytes(block.height),
                EncodingUtils.longToBytes(block.libHeight),
                EncodingUtils.longToBytes(block.weight),
                EncodingUtils.intToBytes(block.diff),
                EncodingUtils.longToBytes(block.round),
                EncodingUtils.longToBytes(block.timestamp),
                block.parentHash.fromHex(),
                block.producerId.fromHex(),
                txHash,
                validatorTxHash,
                block.txOutputHash.fromHex()
            )
        )
    }

    fun hash(block: Block): ByteArray {
        return hash(
            block,
            block.txHash!!.fromHex(),
            block.validatorTxHash.fromHex()
        )
    }

    fun verifyVote(vote: Vote, publicKey: ByteArray): Boolean {
        return CryptoUtils.verify(
            hash(vote),
            vote.signature!!,
            publicKey
        )
    }

    fun hashTransactions(transactions: List<Transaction>): ByteArray {
        val bos = ByteArrayOutputStream()

        transactions
            .sortedBy { it.id }
            .forEach { bos.write(it.signature.fromHex()) }

        return CryptoUtils.hash(bos.toByteArray())
    }

    fun hashVotes(votes: List<Vote>): ByteArray {

        val bos = ByteArrayOutputStream()

        votes
            .sortedBy { it.publicKey }
            .forEach { bos.write(hash(it)) }

        return CryptoUtils.hash(bos.toByteArray())
    }

    fun verifyBlock(block: Block, publicKey: ByteArray): Boolean {
        return CryptoUtils.verify(
            hash(block),
            block.signature.fromHex(),
            publicKey
        )
    }

    fun hashTxOutputs(txOutputs: Map<String, List<PropertyValue>>): ByteArray =
        CryptoUtils.hash(
            txOutputs
                .toSortedMap()
                .map {
                    val valueHash = CryptoUtils.hash(
                        ObjectUtils.writeValueAsString(it.value).toByteArray()
                    )

                    CryptoUtils.hash(Bytes.concat(it.key.fromHex(), valueHash)).toHex()
                }
                .joinToString().toByteArray() // TODO replace with bytes array
        )
}