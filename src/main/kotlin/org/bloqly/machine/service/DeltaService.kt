package org.bloqly.machine.service

import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.vo.block.BlockRangeRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeltaService(
    private val voteRepository: VoteRepository,
    private val spaceService: SpaceService,
    private val blockRepository: BlockRepository
) {

    @Transactional(readOnly = true)
    fun getDeltas(): List<BlockRangeRequest> {
        return spaceService.findAll()
            .filter { blockRepository.existsBySpaceId(it.id) }
            .mapNotNull { space ->
                voteRepository.findLastForSpace(space.id)
            }
            .map { vote ->
                val lastBlock = blockRepository.getLastBlock(vote.spaceId)

                BlockRangeRequest(
                    spaceId = vote.spaceId,
                    startHeight = lastBlock.libHeight + 1,
                    endHeight = vote.height
                )
            }
            .filter { it.startHeight < it.endHeight - 1 }
    }
}