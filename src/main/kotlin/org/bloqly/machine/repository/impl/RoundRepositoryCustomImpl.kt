package org.bloqly.machine.repository.impl

import org.bloqly.machine.repository.RoundRepositoryCustom
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
class RoundRepositoryCustomImpl(
    private var entityManager: EntityManager
) : RoundRepositoryCustom {

}