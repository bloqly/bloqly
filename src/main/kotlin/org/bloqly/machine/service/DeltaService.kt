package org.bloqly.machine.service

import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.vo.BlockRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeltaService(
    private val voteRepository: VoteRepository,
    private val spaceRepository: SpaceRepository,
    private val blockRepository: BlockRepository
) {

    @Transactional(readOnly = true)
    fun getDeltas(): List<BlockRequest> {
        return spaceRepository.findAll()
            .filter { blockRepository.existsBySpaceId(it.id) }
            .map { space ->
                val vote = voteRepository.findLastForSpace(space.id)

                val lastBlock = blockRepository.getLastBlock(space.id)

                BlockRequest(
                    spaceId = space.id,
                    startHeight = lastBlock.height,
                    endHeight = vote.height
                )
            }
            .filter { it.isStale() }
    }
}