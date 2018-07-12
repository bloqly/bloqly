package org.bloqly.machine.service

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.BlockCandidateId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.BlockCandidateRepository
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.PropertyRepository
import org.bloqly.machine.repository.TransactionRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.util.CryptoUtils
import org.bloqly.machine.util.ObjectUtils
import org.bloqly.machine.vo.BlockData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BlockCandidateService(
    private val blockCandidateRepository: BlockCandidateRepository,
    private val transactionRepository: TransactionRepository,
    private val voteRepository: VoteRepository,
    private val propertyRepository: PropertyRepository,
    private val blockRepository: BlockRepository
) {

    fun validateAndSave(blockData: BlockData) {

        if (!validateProposal(blockData)) {
            // TODO it is better to throw things here
            return
        }

        save(blockData)
    }

    fun save(blockData: BlockData) {
        val blockCandidateId = BlockCandidateId(blockData.block.toModel())

        val transactions = blockData.transactions.map { it.toModel() }
        val votes = blockData.votes.map { it.toModel() }

        transactionRepository.saveAll(transactions)
        voteRepository.saveAll(votes)

    }

    private fun validateProposal(blockData: BlockData): Boolean {

        val block = blockData.block

        val votes = blockData.votes

        val lastBlock = blockRepository.getLastBlock(block.spaceId)


        //val votesVerified = votes.all { CryptoUtils.verifyVote(it.toModel()) }

        val transactions = blockData.transactions

        val transactionsVerifiedOK = transactions.all { CryptoUtils.verifyTransaction(it.toModel()) }

        val blockCandidateId = BlockCandidateId(blockData.block.toModel())
        val notExists = !blockCandidateRepository.existsById(blockCandidateId)

        return notExists  && transactionsVerifiedOK
    }

    fun getBlockCandidate(space: Space, height: Long, producer: Account): BlockData? {
        return blockCandidateRepository.getBlockCandidate(space.id, height, producer.id)
            ?.let {
                ObjectUtils.readValue(it.data, BlockData::class.java)
            }
    }

    fun getBestBlockCandidate(space: Space, height: Long): BlockData? {
        return blockCandidateRepository.getBlockCandidate(space.id, height)
            ?.let {
                val blockData = ObjectUtils.readValue(it.data, BlockData::class.java)
                val quorum = propertyRepository.getQuorumBySpace(space)

                blockData.takeIf { blockData.votes.size >= quorum }
            }
    }
}