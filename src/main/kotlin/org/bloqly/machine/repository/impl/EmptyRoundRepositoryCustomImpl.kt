package org.bloqly.machine.repository.impl

import org.bloqly.machine.model.EmptyRound
import org.bloqly.machine.model.RoundId
import org.bloqly.machine.repository.EmptyRoundRepositoryCustom
import org.springframework.stereotype.Repository
import java.time.Instant
import javax.persistence.EntityManager

@Repository
class EmptyRoundRepositoryCustomImpl(
    private var entityManager: EntityManager
) : EmptyRoundRepositoryCustom {

    override fun processEmptyRound(spaceId: String, height: Long) {
        val roundId = RoundId(spaceId, height)

        val emptyRoundId = entityManager.find(EmptyRound::class.java, roundId)
            ?: EmptyRound(id = roundId, counter = 0, lastMissTime = 0)

        entityManager.merge(
            emptyRoundId.copy(
                counter = emptyRoundId.counter + 1,
                lastMissTime = Instant.now().toEpochMilli()
            )
        )
    }
}