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
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.Delta
import org.springframework.stereotype.Service
import javax.transaction.Transactional
import kotlin.math.min

@Service
@Transactional
class BlockService(
    private val accountRepository: AccountRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val propertyRepository: PropertyRepository
) {

    fun newBlock(
        spaceId: String,
        height: Long,
        weight: Long,
        diff: Int,
        timestamp: Long,
        parentHash: String,
        producerId: String,
        txHash: ByteArray? = null,
        validatorTxHash: ByteArray,
        round: Long,
        transactions: List<Transaction> = listOf(),
        votes: List<Vote> = listOf()
    ): Block {

        return accountRepository
            .findById(producerId)
            .filter { it.hasKey() }
            .map { proposer ->

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

                val privateKey = proposer.privateKey.decode16()
                val signature = CryptoUtils.sign(privateKey, dataToSign)
                val blockHash = CryptoUtils.hash(signature).encode16()

                val libHash = if (height > 0) getLIBForSpace(spaceId, producerId).hash else ""

                Block(
                    spaceId = spaceId,
                    height = height,
                    weight = weight,
                    diff = diff,
                    round = round,
                    timestamp = timestamp,
                    parentHash = parentHash,
                    producerId = producerId,
                    txHash = txHash,
                    validatorTxHash = validatorTxHash,
                    signature = signature,
                    transactions = transactions,
                    votes = votes,
                    hash = blockHash,
                    libHash = libHash
                )
            }
            .orElseThrow {
                IllegalArgumentException("Could not create block in behalf of producer $producerId")
            }
    }

    fun getLastBlockForSpace(spaceId: String): Block {
        return blockRepository.getLastBlock(spaceId)
    }

    fun getLIBForSpace(spaceId: String, newBlockValidatorId: String? = null): Block {

        val quorum = propertyRepository.getQuorumBySpaceId(spaceId)

        val validatorIds = mutableSetOf<String>()

        newBlockValidatorId?.let { validatorIds.add(it) }

        var block = blockRepository.getLastBlock(spaceId)

        if (block.height > 0) {
            validatorIds.add(block.producerId)
        }

        while (validatorIds.size < quorum && block.height > 0) {
            block = blockRepository.findByHash(block.parentHash)!!

            validatorIds.add(block.producerId)
        }

        return if (block.height > 0) {
            blockRepository.findByHash(block.parentHash)!!
        } else {
            block
        }
    }

    fun getBlockDataList(delta: Delta): BlockDataList {

        val startHeight = delta.localHeight
        val endHeight = min(delta.remoteHeight, startHeight + MAX_DELTA_SIZE)

        val blocks = blockRepository.getBlocksDelta(delta.spaceId, startHeight, endHeight)

        return BlockDataList(blocks.map { BlockData(it) })
    }

    fun ensureSpaceEmpty(space: String) {

        require(!blockRepository.existsBySpaceId(space)) {
            "Blockchain already initialized for spaceId '$space'"
        }

        require(!spaceRepository.existsById(space)) {
            "Space '$space' already exists"
        }
    }
}