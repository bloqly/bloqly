package org.bloqly.machine.repository.impl

import org.bloqly.machine.Application
import org.bloqly.machine.model.Property
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.repository.PropertyRepositoryCustom
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
class PropertyRepositoryCustomImpl(
    private var entityManager: EntityManager
) : PropertyRepositoryCustom {

    override fun getQuorum(space: String): Int {

        val quorumProperty = entityManager.find(
            Property::class.java, PropertyId(
                space = space,
                self = Application.DEFAULT_SELF,
                target = Application.DEFAULT_SELF,
                key = Application.QUORUM_KEY
            )
        )

        return ParameterUtils.readValue(quorumProperty.value) as Int
    }
}