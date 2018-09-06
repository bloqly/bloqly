package org.bloqly.machine.repository.impl

import org.bloqly.machine.Application
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.PropertyRepositoryCustom
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
class PropertyRepositoryCustomImpl(
    val entityManager: EntityManager
) : PropertyRepositoryCustom {

    override fun getQuorumBySpace(space: Space): Int {
        return getQuorumBySpaceId(space.id)
    }

    override fun getQuorumBySpaceId(spaceId: String): Int {

        val quorumProperty = entityManager.find(
            Property::class.java, PropertyId(
                spaceId = spaceId,
                self = Application.DEFAULT_SELF,
                target = Application.DEFAULT_SELF,
                key = Application.QUORUM_KEY
            )
        )

        return ParameterUtils.readValue(quorumProperty.value) as Int
    }

    override fun getValidatorsCountSpaceId(spaceId: String): Int {

        val validatorsCount = entityManager.find(
            Property::class.java, PropertyId(
                spaceId = spaceId,
                self = Application.DEFAULT_SELF,
                target = Application.DEFAULT_SELF,
                key = Application.VALIDATORS_KEY
            )
        )

        return ParameterUtils.readValue(validatorsCount.value) as Int
    }
}