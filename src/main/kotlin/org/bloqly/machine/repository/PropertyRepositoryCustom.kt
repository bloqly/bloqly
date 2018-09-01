package org.bloqly.machine.repository

import org.bloqly.machine.model.Space

interface PropertyRepositoryCustom {

    fun getQuorumBySpace(space: Space): Int

    fun getQuorumBySpaceId(spaceId: String): Int

    fun getValidatorsCountSpaceId(spaceId: String): Int
}