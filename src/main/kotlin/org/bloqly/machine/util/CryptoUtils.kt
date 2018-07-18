package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bouncycastle.util.BigIntegers
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object CryptoUtils {

    private val log = LoggerFactory.getLogger(CryptoUtils::class.simpleName)

    private const val SHA_256 = "SHA-256"

    fun newPrivateKey(): ByteArray = BloqlySchnorr.newPrivateKey()

    fun getPublicFor(privateKeyBytes: ByteArray): ByteArray {

        val privateKey = BigIntegers.fromUnsignedByteArray(privateKeyBytes)

        return BloqlySchnorr.getPublicFromPrivate(privateKey)
    }

    fun hash(inputs: Array<ByteArray>): ByteArray {

        ByteArrayOutputStream().use { bos ->

            for (input in inputs) {
                bos.write(input)
            }

            return MessageDigest.getInstance(SHA_256)
                .digest(bos.toByteArray())
        }
    }

    fun hash(input: ByteArray): ByteArray {
        return MessageDigest.getInstance(SHA_256).digest(input)
    }

    fun hash(input: String): ByteArray {
        return hash(input.toByteArray())
    }

    fun digestTransactions(transactions: List<Transaction>): ByteArray {
        val bos = ByteArrayOutputStream()

        transactions
            .sortedBy { it.id }
            .forEach { bos.write(it.signature.decode64()) }

        return hash(bos.toByteArray())
    }

    fun digestVotes(votes: List<Vote>): ByteArray {

        val bos = ByteArrayOutputStream()

        votes
            .sortedBy { it.validator.accountId }
            .forEach { bos.write(hash(it)) }

        return hash(bos.toByteArray())
    }

    fun sign(privateKey: ByteArray, input: ByteArray): ByteArray =
        BloqlySchnorr.sign(input, privateKey).toByteArray()

    fun hash(tx: Transaction): ByteArray {
        return hash(
            Bytes.concat(
                tx.spaceId.toByteArray(),
                tx.origin.toByteArray(),
                tx.destination.toByteArray(),
                tx.self.toByteArray(),
                tx.key?.toByteArray() ?: byteArrayOf(),
                tx.value.decode64(),
                tx.referencedBlockHash.decode16(),
                tx.transactionType.name.toByteArray(),
                EncodingUtils.longToBytes(tx.timestamp)
            )
        )
    }

    fun verifyTransaction(tx: Transaction): Boolean {
        try {
            val signature = tx.signature.decode64()

            val txHash = hash(signature).encode16()

            if (txHash != tx.hash) {
                return false
            }

            val txDataHash = hash(tx)

            return verify(
                message = txDataHash,
                signature = signature,
                publicKey = tx.publicKey.decode16()
            )
        } catch (e: Exception) {
            log.error(e.message, e)
            return false
        }
    }

    fun hash(vote: Vote): ByteArray {
        return hash(
            Bytes.concat(
                vote.validator.accountId.decode16(),
                vote.blockHash.decode16(),
                EncodingUtils.longToBytes(vote.height),
                vote.spaceId.toByteArray(),
                EncodingUtils.longToBytes(vote.timestamp)
            )
        )
    }

    fun verifyVote(vote: Vote, publicKey: ByteArray): Boolean {
        return verify(hash(vote), vote.signature!!, publicKey)
    }

    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        if (signature.isEmpty()) {
            return false
        }
        return BloqlySchnorr.verify(message, Signature.fromByteArray(signature), publicKey)
    }
}
