package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.MAX_DELTA_SIZE
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.util.encode64
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.Delta
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional
import kotlin.math.min

@Service
@Transactional(isolation = SERIALIZABLE)
class BlockService(
    private val accountRepository: AccountRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val propertyRepository: PropertyRepository
) {

    @Transactional(readOnly = true)
    fun newBlock(
        spaceId: String,
        height: Long,
        weight: Long,
        diff: Int,
        timestamp: Long,
        parentHash: String,
        producerId: String,
        passphrase: String,
        txHash: ByteArray? = null,
        validatorTxHash: ByteArray,
        round: Long,
        transactions: List<Transaction> = listOf(),
        votes: List<Vote> = listOf()
    ): Block {

        val proposer = accountRepository.findByAccountId(producerId)
            ?: throw IllegalArgumentException("Could not find producer: $producerId")

        val dataToSign = CryptoUtils.hash(
            arrayOf(
                spaceId.toByteArray(),
                EncodingUtils.longToBytes(height),
                EncodingUtils.longToBytes(weight),
                EncodingUtils.intToBytes(diff),
                EncodingUtils.longToBytes(round),
                EncodingUtils.longToBytes(timestamp),
                parentHash.toByteArray(),
                producerId.toByteArray(),
                txHash ?: ByteArray(0),
                validatorTxHash
            )
        )

        val signature = CryptoUtils.sign(
            CryptoUtils.decrypt(proposer.privateKeyEncoded, passphrase),
            dataToSign
        )
        val blockHash = CryptoUtils.hash(signature).encode16()

        val libHash = if (height > 0) getLIBForSpace(spaceId, producerId).hash else ""

        return Block(
            spaceId = spaceId,
            height = height,
            weight = weight,
            diff = diff,
            round = round,
            timestamp = timestamp,
            parentHash = parentHash,
            producerId = producerId,
            txHash = txHash?.encode64(),
            validatorTxHash = validatorTxHash.encode64(),
            signature = signature.encode64(),
            transactions = transactions,
            votes = votes,
            hash = blockHash,
            libHash = libHash
        )
    }

    @Transactional(readOnly = true)
    fun getLastBlockForSpace(spaceId: String): Block {
        return blockRepository.getLastBlock(spaceId)
    }

    /**
     * @param newBlockValidatorId - when creating a new block, we need to include LIB value into it.
     *      this LIB value can differ so that a new block's LIB is the the previous LIB + 1 because
     *      new block introduces new validator into the chain of confirmations.
     */
    @Transactional(readOnly = true)
    fun getLIBForSpace(spaceId: String, newBlockValidatorId: String? = null): Block {

        val quorum = propertyRepository.getQuorumBySpaceId(spaceId)

        // check if we have "hyper-confirmation"

        var block = blockRepository.getLastBlock(spaceId)

        val validatorIds = mutableSetOf<String>()

        newBlockValidatorId?.let { validatorIds.add(it) }

        while (validatorIds.size < quorum && block.height > 0) {

            validatorIds.add(block.producerId)

            block = blockRepository.findByHash(block.parentHash)!!
        }

        return block
    }

    @Transactional(readOnly = true)
    fun getBlockDataList(delta: Delta): BlockDataList {

        val startHeight = delta.localHeight
        val endHeight = min(delta.remoteHeight, startHeight + MAX_DELTA_SIZE)

        val blocks = blockRepository.getBlocksDelta(delta.spaceId, startHeight, endHeight)

        return BlockDataList(blocks.map { BlockData(it) })
    }

    @Transactional(readOnly = true)
    fun ensureSpaceEmpty(space: String) {

        require(!blockRepository.existsBySpaceId(space)) {
            "Blockchain already initialized for spaceId '$space'"
        }

        require(!spaceRepository.existsById(space)) {
            "Space '$space' already exists"
        }
    }

    @Transactional(readOnly = true)
    fun loadBlockByHash(hash: String): Block {
        val block = blockRepository.findByHash(hash)!!

        block.transactions.size
        block.votes.size

        return block
    }
}