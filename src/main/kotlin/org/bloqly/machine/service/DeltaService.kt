package org.bloqly.machine.service

import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.repository.VoteRepository
import org.bloqly.machine.vo.Delta
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(isolation = SERIALIZABLE)
class DeltaService(
    private val voteRepository: VoteRepository,
    private val spaceRepository: SpaceRepository,
    private val blockRepository: BlockRepository
) {

    @Transactional(readOnly = true)
    fun getDeltas(): List<Delta> {
        val spaces = spaceRepository.findAll()

        return spaces
            .map { space ->
                val vote = voteRepository.findLastForSpace(space.id)

                val lastBlock = blockRepository.getLastBlock(space.id)

                Delta(
                    spaceId = space.id,
                    localHeight = lastBlock.height,
                    remoteHeight = vote.height
                )
            }
            .filter { it.isStale() }
    }
}