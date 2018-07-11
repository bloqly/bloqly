package org.bloqly.machine.service

import org.bloqly.machine.Application.Companion.MAX_DELTA_SIZE
import org.bloqly.machine.model.Block
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
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
    private val transactionRepository: TransactionRepository,
    private val voteRepository: VoteRepository,
    private val spaceRepository: SpaceRepository
) {

    fun newBlock(
        spaceId: String,
        height: Long,
        weight: Long,
        diff: Int,
        timestamp: Long,
        parentId: String,
        producerId: String,
        txHash: ByteArray? = null,
        validatorTxHash: ByteArray,
        round: Long
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
                        parentId.toByteArray(),
                        producerId.toByteArray(),
                        txHash ?: ByteArray(0),
                        validatorTxHash
                    )
                )

                val privateKey = proposer.privateKey.decode16()
                val signature = CryptoUtils.sign(privateKey, dataToSign)
                val blockHash = CryptoUtils.hash(signature)
                val blockId = blockHash.encode16()

                Block(
                    id = blockId,
                    spaceId = spaceId,
                    height = height,
                    weight = weight,
                    diff = diff,
                    round = round,
                    timestamp = timestamp,
                    parentId = parentId,
                    proposerId = producerId,
                    txHash = txHash,
                    validatorTxHash = validatorTxHash,
                    signature = signature
                )
            }
            .orElseThrow {
                IllegalArgumentException("Could not create block in behalf of producer $producerId")
            }
    }

    fun getLastBlockForSpace(spaceId: String): Block {
        return blockRepository.getLastBlock(spaceId)
    }

    fun getBlockDataList(delta: Delta): BlockDataList {

        val startHeight = delta.localHeight
        val endHeight = min(delta.remoteHeight, startHeight + MAX_DELTA_SIZE)

        val blocks = blockRepository.getBlocksDelta(delta.spaceId, startHeight, endHeight)

        return BlockDataList(blocks.map { block ->

            val transactions = transactionRepository.findByContainingBlockId(block.id)
            val votes = voteRepository.findByBlockId(block.parentId)

            BlockData(block, transactions, votes)
        })
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