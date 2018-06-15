package org.bloqly.machine.service

import org.bloqly.machine.model.Delta
import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.VoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeltaService(
    private val voteRepository: VoteRepository,
    private val spaceRepository: SpaceRepository,
    private val blockRepository: BlockRepository
) {

    fun getDeltas(): List<Delta> {
        val spaces = spaceRepository.findAll()

        return spaces
            .map { space ->
                val vote = voteRepository.findLastForSpace(space.id)
                val lastBlock = blockRepository.findFirstBySpaceOrderByHeightDesc(space.id)

                Delta(
                    spaceId = space.id,
                    localHeight = lastBlock.height,
                    remoteHeight = vote.id.height
                )
            }
            .filter { it.isStale() }
    }
}