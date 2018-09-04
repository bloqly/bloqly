package org.bloqly.machine.repository

import org.bloqly.machine.model.Space
import org.springframework.cache.annotation.Cacheable

interface PropertyRepositoryCustom {

    fun getQuorumBySpace(space: Space): Int

    @Cacheable("quorum")
    fun getQuorumBySpaceId(spaceId: String): Int

    fun getValidatorsCountSpaceId(spaceId: String): Int
}