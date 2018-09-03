package org.bloqly.machine.util

import com.google.common.primitives.Bytes
import org.bitcoinj.core.ECKey
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bouncycastle.util.BigIntegers
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private val log = LoggerFactory.getLogger(CryptoUtils::class.simpleName)

    private const val SHA_256 = "SHA-256"
    private const val AES = "AES"
    private const val AES_PADDING = "AES/CBC/PKCS5Padding"
    private const val AES_IV_SIZE = 16
    private const val AES_INPUT_SIZE = 32

    fun encrypt(input: ByteArray?, passphrase: String): ByteArray {
        require(input != null)
        require(input!!.size == AES_INPUT_SIZE)
        require(passphrase.isNotEmpty())

        val ivSize = AES_IV_SIZE
        val iv = ByteArray(ivSize)
        val random = SecureRandom()
        random.nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)

        val key = SecretKeySpec(hash(passphrase), AES)
        val cipher = Cipher.getInstance(AES_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec)

        val encrypted = cipher.doFinal(input)

        val encryptedIVAndText = ByteArray(ivSize + encrypted.size)
        System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize)
        System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.size)

        return encryptedIVAndText
    }

    fun decrypt(input: ByteArray?, passphrase: String): ByteArray {
        require(input != null)

        val iv = ByteArray(AES_IV_SIZE)
        System.arraycopy(input, 0, iv, 0, iv.size)
        val ivParameterSpec = IvParameterSpec(iv)

        val encryptedSize = input!!.size - AES_IV_SIZE
        val encryptedBytes = ByteArray(encryptedSize)
        System.arraycopy(input, AES_IV_SIZE, encryptedBytes, 0, encryptedSize)

        val key = SecretKeySpec(hash(passphrase), AES)
        val cipher = Cipher.getInstance(AES_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec)
        return cipher.doFinal(encryptedBytes)
    }

    fun getPublicFor(privateKeyBytes: ByteArray?): ByteArray {

        val privateKey = BigIntegers.fromUnsignedByteArray(privateKeyBytes)

        return ECKey.publicKeyFromPrivate(privateKey, true)
    }

    fun hash(input: ByteArray): ByteArray {
        return MessageDigest.getInstance(SHA_256).digest(input)
    }

    fun hash(input: String): ByteArray {
        return hash(input.toByteArray())
    }

    fun hashTransactions(transactions: List<Transaction>): ByteArray {
        val bos = ByteArrayOutputStream()

        transactions
            .sortedBy { it.id }
            .forEach { bos.write(it.signature.decode16()) }

        return hash(bos.toByteArray())
    }

    fun hashVotes(votes: List<Vote>): ByteArray {

        val bos = ByteArrayOutputStream()

        votes
            .sortedBy { it.publicKey }
            .forEach { bos.write(hash(it)) }

        return hash(bos.toByteArray())
    }

    fun sign(privateKey: ByteArray?, input: ByteArray): ByteArray =
        BloqlySchnorr.sign(input, privateKey!!).toByteArray()

    fun hash(tx: Transaction): ByteArray {
        return hash(
            Bytes.concat(
                tx.spaceId.toByteArray(),
                tx.origin.toByteArray(),
                tx.destination.toByteArray(),
                tx.self.toByteArray(),
                tx.key?.toByteArray() ?: byteArrayOf(),
                tx.value.decode16(),
                tx.referencedBlockHash.decode16(),
                tx.transactionType.name.toByteArray(),
                EncodingUtils.longToBytes(tx.timestamp)
            )
        )
    }

    fun verifyTransaction(tx: Transaction): Boolean {
        try {
            val signature = tx.signature.decode16()

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
            log.warn(e.message)
            log.error(e.message, e)
            return false
        }
    }

    fun hash(vote: Vote): ByteArray {
        return hash(
            Bytes.concat(
                vote.publicKey.decode16(),
                vote.blockHash.decode16(),
                EncodingUtils.longToBytes(vote.height),
                vote.spaceId.toByteArray(),
                EncodingUtils.longToBytes(vote.timestamp)
            )
        )
    }

    fun hash(block: Block, txHash: ByteArray = ByteArray(0), validatorTxHash: ByteArray): ByteArray {
        return hash(
            Bytes.concat(
                block.spaceId.toByteArray(),
                EncodingUtils.longToBytes(block.height),
                EncodingUtils.longToBytes(block.libHeight),
                EncodingUtils.longToBytes(block.weight),
                EncodingUtils.intToBytes(block.diff),
                EncodingUtils.longToBytes(block.round),
                EncodingUtils.longToBytes(block.timestamp),
                block.parentHash.toByteArray(),
                block.producerId.toByteArray(),
                txHash,
                validatorTxHash
            )
        )
    }

    fun hash(block: Block): ByteArray {
        return hash(block, block.txHash!!.decode16(), block.validatorTxHash.decode16())
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

    fun verifySchnorrHex(message: String, signature: String, publicKey: String): Boolean =
        verify(
            message.decode16(),
            signature.decode16(),
            publicKey.decode16()
        )

    fun verifyHex(message: String, signature: String, publicKey: String): Boolean =
        ECKey.verify(
            message.decode16(),
            signature.decode16(),
            publicKey.decode16()
        )

    fun verifyBlock(block: Block, publicKey: ByteArray): Boolean {
        return verify(
            hash(block),
            block.signature.decode16(),
            publicKey
        )
    }
}
