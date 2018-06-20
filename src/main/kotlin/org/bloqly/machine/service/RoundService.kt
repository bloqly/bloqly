package org.bloqly.machine.service

import org.bloqly.machine.model.Round
import org.bloqly.machine.model.RoundId
import org.bloqly.machine.repository.RoundRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RoundService(
    private val roundRepository: RoundRepository
) {

    fun save(round: Round) {
        roundRepository.save(round)
    }

    fun createZeroRound(space: String, rootId: String) {
        val round = Round(
            id = RoundId(
                spaceId = space,
                round = 0
            ),
            producerId = rootId,
            createTime = Instant.now().toEpochMilli()
        )

        roundRepository.save(round)
    }
}