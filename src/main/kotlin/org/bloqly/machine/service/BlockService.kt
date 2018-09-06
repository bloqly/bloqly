package org.bloqly.machine.service

import com.google.common.primitives.Bytes
import org.bloqly.machine.Application.Companion.MAX_DELTA_SIZE
import org.bloqly.machine.Application.Companion.MAX_REFERENCED_BLOCK_DEPTH
import org.bloqly.machine.model.Block
import org.bloqly.machine.model.Space
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.AccountRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.TransactionOutputRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.block.BlockData
import org.bloqly.machine.vo.block.BlockDataList
import org.bloqly.machine.vo.block.BlockRangeRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.min

@Service
class BlockService(
    private val accountRepository: AccountRepository,
    private val blockRepository: BlockRepository,
    private val spaceRepository: SpaceRepository,
    private val propertyRepository: PropertyRepository,
    private val transactionOutputRepository: TransactionOutputRepository
) {

    private val log: Logger = LoggerFactory.getLogger(BlockService::class.simpleName)

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

        val newBlock = Block(
            spaceId = spaceId,
            height = height,
            weight = weight,
            diff = diff,
            round = round,
            timestamp = timestamp,
            parentHash = parentHash,
            producerId = producerId,
            txHash = txHash?.encode16(),
            validatorTxHash = validatorTxHash.encode16(),
            transactions = transactions,
            votes = votes
        )

        val libHeight = if (height > 0) {
            calculateLIBForBlock(newBlock).height
        } else {
            0
        }

        val dataToSign = Bytes.concat(
            spaceId.toByteArray(),
            EncodingUtils.longToBytes(height),
            EncodingUtils.longToBytes(libHeight),
            EncodingUtils.longToBytes(weight),
            EncodingUtils.intToBytes(diff),
            EncodingUtils.longToBytes(round),
            EncodingUtils.longToBytes(timestamp),
            parentHash.toByteArray(),
            producerId.toByteArray(),
            txHash ?: ByteArray(0),
            validatorTxHash
        )

        val signature = CryptoUtils.sign(
            CryptoUtils.decrypt(proposer.privateKeyEncoded, passphrase),
            CryptoUtils.hash(dataToSign)
        )
        val blockHash = CryptoUtils.hash(signature).encode16()

        return newBlock.copy(
            hash = blockHash,
            libHeight = libHeight,
            signature = signature.encode16()
        )
    }

    @Transactional(readOnly = true)
    fun getLastBlockBySpace(spaceId: String): Block =
        blockRepository.getLastBlock(spaceId)

    @Transactional(readOnly = true)
    fun existsByHash(hash: String): Boolean =
        blockRepository.existsByHash(hash)

    @Transactional(readOnly = true)
    fun isAfterLIB(block: Block): Boolean {

        if (block.height == 0L) {
            return true
        }

        val lastBlock = blockRepository.getLastBlock(block.spaceId)

        var currentBlock = block

        while (currentBlock.height > lastBlock.libHeight) {

            if (currentBlock.hash == block.hash) {
                return true
            }

            currentBlock = findByHash(currentBlock.parentHash) ?: return false
        }

        return false
    }

    @Transactional(readOnly = true)
    fun isAcceptable(block: Block): Boolean {

        if (blockRepository.existsByHash(block.hash)) {
            log.warn("Block hash already exists: ${block.hash}")
            return false
        }

        if (!blockRepository.existsByHash(block.parentHash)) {
            log.warn("No parent found with hash ${block.parentHash} for block ${block.hash}.")
            return false
        }

        if (blockRepository.existsBySpaceIdAndProducerIdAndHeight(block.spaceId, block.producerId, block.height)) {
            log.warn("Unique constraint violated (space_id, producer_id, height) : (${block.spaceId}, ${block.producerId}, ${block.height})")
            return false
        }

        if (blockRepository.existsBySpaceIdAndProducerIdAndRound(block.spaceId, block.producerId, block.round)) {
            log.warn("Unique constraint violated (space_id, producer_id, round) : (${block.spaceId}, ${block.producerId}, ${block.round})")
            return false
        }

        if (!isAfterLIB(block)) {
            log.warn("Proposed block is from different branch: ${block.hash}")
            return false
        }

        val lib = calculateLIBForBlock(block)

        if (lib.height != block.libHeight) {
            log.warn("Wrong LIB. Expected ${lib.height}, proposed block: ${block.header()}")
            return false
        }

        return true
    }

    @Transactional(readOnly = true)
    fun calculateLIBForBlock(blockHash: String): Block = calculateLIBForBlock(getByHash(blockHash))

    @Transactional(readOnly = true)
    fun calculateLIBForBlock(block: Block): Block {

        if (block.height == 0L) {
            return block
        }

        val quorum = propertyRepository.getQuorumBySpaceId(block.spaceId)

        log.info("Calculating LIB for block ${block.header()}")

        val validatorVotesCount = mutableMapOf<String, Int>()

        val parentBlock = getByHash(block.parentHash)

        var currentBlock = block

        while (currentBlock.height > 0 && currentBlock.height > parentBlock.libHeight) {

            validatorVotesCount.compute(currentBlock.producerId) { _, count ->
                calculateVotesCount(count)
            }

            if (getCommitsCount(validatorVotesCount) >= quorum) {
                break
            }

            currentBlock.votes.forEach { vote ->
                validatorVotesCount.compute(EncodingUtils.publicKeyToAddress(vote.publicKey)) { _, count ->
                    calculateVotesCount(count)
                }
            }

            currentBlock = blockRepository.getByHash(currentBlock.parentHash)
        }

        return currentBlock
    }

    private fun calculateVotesCount(count: Int?): Int =
        if (count == null) {
            1
        } else {
            count + 1
        }

    private fun getCommitsCount(validatorVotesCount: Map<String, Int>): Int =
        validatorVotesCount.filter { it.value > 1 }.size

    @Transactional(readOnly = true)
    fun getBlockDataList(blockRangeRequest: BlockRangeRequest): BlockDataList {

        val startHeight = blockRangeRequest.startHeight
        val endHeight = min(blockRangeRequest.endHeight, startHeight + MAX_DELTA_SIZE)

        val blocks = blockRepository.getBlocksDelta(blockRangeRequest.spaceId, startHeight, endHeight)

        return BlockDataList(blocks.map { block ->
            BlockData(
                block = block,
                transactionOutputs = transactionOutputRepository.findByBlockHash(block.hash)
            )
        })
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
    fun isActualTransaction(tx: Transaction, depth: Int = MAX_REFERENCED_BLOCK_DEPTH): Boolean {

        return blockRepository.findByHash(tx.referencedBlockHash)
            ?.let { referencedBlock ->

                val lastBlock = blockRepository.getLastBlock(tx.spaceId)

                val actualDepth = lastBlock.height - referencedBlock.height

                actualDepth <= depth
            } ?: false
    }

    @Transactional(readOnly = true)
    fun findByHash(hash: String): Block? = blockRepository.findByHash(hash)

    @Transactional(readOnly = true)
    fun getByHash(hash: String): Block = blockRepository.getByHash(hash)

    @Transactional(readOnly = true)
    fun existsBySpace(space: Space): Boolean = blockRepository.existsBySpaceId(space.id)

    @Transactional(readOnly = true)
    fun existsBySpaceId(spaceId: String): Boolean {
        return spaceRepository.findById(spaceId).map { space ->
            blockRepository.existsBySpaceId(space.id)
        }.orElse(false)
    }

    @Transactional(readOnly = true)
    fun getLIBForBlock(block: Block): Block {
        if (block.height == 0L) {
            return block
        }

        var currentBlock = block

        while (currentBlock.height > block.libHeight) {
            currentBlock = blockRepository.getByHash((currentBlock.parentHash))
        }

        return currentBlock
    }
}