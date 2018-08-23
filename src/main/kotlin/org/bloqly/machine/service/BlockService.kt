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
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.EncodingUtils
import org.bloqly.machine.util.encode16
import org.bloqly.machine.vo.BlockData
import org.bloqly.machine.vo.BlockDataList
import org.bloqly.machine.vo.BlockRequest
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
    private val propertyRepository: PropertyRepository
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

        val dataToSign = Bytes.concat(
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

        val signature = CryptoUtils.sign(
            CryptoUtils.decrypt(proposer.privateKeyEncoded, passphrase),
            CryptoUtils.hash(dataToSign)
        )
        val blockHash = CryptoUtils.hash(signature).encode16()

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
            signature = signature.encode16(),
            transactions = transactions,
            votes = votes,
            hash = blockHash
        )

        val libHash = if (height > 0) {
            calculateLIBForBlock(newBlock).hash
        } else {
            blockHash
        }

        return newBlock.copy(libHash = libHash)
    }

    @Transactional(readOnly = true)
    fun getLastBlockBySpace(spaceId: String): Block =
        blockRepository.getLastBlock(spaceId)

    @Transactional(readOnly = true)
    fun getLastBlockDataBySpace(space: Space): BlockData =
        BlockData(blockRepository.getLastBlock(space.id))

    @Transactional(readOnly = true)
    fun existsByHash(hash: String): Boolean =
        blockRepository.existsByHash(hash)

    @Transactional(readOnly = true)
    fun isAfterLIB(target: Block): Boolean {

        val lastBlock = blockRepository.getLastBlock(target.spaceId)

        val lib = blockRepository.getByHash(lastBlock.libHash)

        var block = target

        while (block.hash != lib.hash && block.height > 0 && block.height >= lib.height) {
            block = blockRepository.findByHash(block.parentHash) ?: return false
        }

        return block.hash == lib.hash
    }

    @Transactional(readOnly = true)
    fun isAcceptable(block: Block): Boolean {

        if (!isAfterLIB(block)) {
            log.warn("Proposed block is from different branch: ${block.hash}")
            return false
        }

        if (blockRepository.existsByHash(block.hash)) {
            log.warn("Block hash already exists: ${block.hash}")
            return false
        }

        if (!blockRepository.existsByHash(block.parentHash)) {
            log.warn("No parent found with hash ${block.parentHash} for block ${block.hash}.")
            return false
        }

        if (!blockRepository.existsByHash(block.libHash)) {
            log.warn("No LIB found by hash ${block.libHash}.")
            return false
        }

        if (blockRepository.existsByHashAndLibHash(block.hash, block.libHash)) {
            log.warn("Unique constraint violated (hash, block_hash) : (${block.hash}, ${block.libHash})")
            return false
        }

        if (blockRepository.existsByHashAndParentHash(block.hash, block.parentHash)) {
            log.warn("Unique constraint violated (hash, parent_hash) : (${block.hash}, ${block.parentHash})")
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

        return true
    }

    @Transactional(readOnly = true)
    fun calculateLIBForBlock(targetBlock: Block): Block {

        if (targetBlock.height == 0L) {
            return targetBlock
        }

        // TODO calculate quorum taking into account the current block producer
        // also, being a block producer, don't create a vote as it is unnecessary -
        // you already vote with your block
        // introduce method "isQuorum(block: Block): Boolean"
        val quorum = propertyRepository.getQuorumBySpaceId(targetBlock.spaceId)

        if (isHyperFinalizer(targetBlock, quorum)) {
            return blockRepository.findByHash(targetBlock.parentHash)!!
        }

        log.info("Calculating LIB for block ${targetBlock.header()}")

        val validatorIds = mutableSetOf<String>()

        var block = targetBlock

        while (validatorIds.size < quorum && block.height > 0) {

            validatorIds.add(block.producerId)

            log.info("Getting parent for block ${block.header()}")
            block = blockRepository.getByHash(block.parentHash)
        }

        return block
    }

    @Transactional(readOnly = true)
    fun isHyperFinalizer(currBlock: Block, quorum: Int): Boolean {

        if (currBlock.height < 2) {
            return false
        }

        // TODO bug, returns not unique results
        val prevBlock = blockRepository.findByHash(currBlock.parentHash)!!

        val prevBlockValidators = prevBlock.votes.map { it.validator.accountId }.toSet()
        val currBlockValidators = currBlock.votes.map { it.validator.accountId }.toSet()

        return currBlockValidators.intersect(prevBlockValidators).size >= quorum
    }

    @Transactional(readOnly = true)
    fun getBlockDataList(blockRequest: BlockRequest): BlockDataList {

        val startHeight = blockRequest.startHeight
        val endHeight = min(blockRequest.endHeight, startHeight + MAX_DELTA_SIZE)

        val blocks = blockRepository.getBlocksDelta(blockRequest.spaceId, startHeight, endHeight)

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
}