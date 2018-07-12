package org.bloqly.machine.component

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Transaction
import org.bloqly.machine.model.Vote
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.service.BlockService
import org.bloqly.machine.service.TransactionService
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class BlockProcessor(
    private val transactionService: TransactionService,
    private val voteRepository: VoteRepository,
    private val blockService: BlockService,
    private val blockRepository: BlockRepository,
    private val transactionProcessor: TransactionProcessor
) {

    fun createNextBlock(spaceId: String, producer: Account, round: Long): BlockData {

        val existingBlock = blockRepository.findBySpaceIdAndProposerIdAndRound(spaceId, producer.id, round)

        if (existingBlock != null) {
            return BlockData(existingBlock)
        }

        val lastBlock = blockService.getLastBlockForSpace(spaceId)

        val newHeight = lastBlock.height + 1
        val votes = getVotesForBlock(lastBlock.id)
        val prevVotes = getVotesForBlock(lastBlock.parentId)

        val diff = votes.minus(prevVotes).size

        val transactions = getPendingTransactions(spaceId)

        val weight = lastBlock.weight + votes.size

        val newBlock = blockService.newBlock(
            spaceId = spaceId,
            height = newHeight,
            weight = weight,
            diff = diff,
            timestamp = Instant.now().toEpochMilli(),
            parentId = lastBlock.id,
            producerId = producer.id,
            txHash = CryptoUtils.digestTransactions(transactions),
            validatorTxHash = CryptoUtils.digestVotes(votes),
            round = round,
            transactions = transactions,
            votes = votes
        )

        return BlockData(
            blockRepository.save(newBlock))
    }

    private fun getPendingTransactions(spaceId: String): List<Transaction> {
        return transactionService.getPendingTransactionsBySpace(spaceId)
    }

    private fun getVotesForBlock(blockId: String): List<Vote> {
        return voteRepository.findByBlockId(blockId)
    }
}