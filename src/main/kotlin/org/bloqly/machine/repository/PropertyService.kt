package org.bloqly.machine.repository

import org.bloqly.machine.Application
import org.bloqly.machine.model.PropertyId
import org.bloqly.machine.util.ParameterUtils
import org.springframework.stereotype.Service

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository
) {
    fun getQuorum(space: String): Int {
        val quorumProperty = propertyRepository.findById(
                PropertyId(
                        space = space,
                        self = Application.DEFAULT_SELF,
                        target = Application.DEFAULT_SELF,
                        key = Application.QUORUM_KEY
                )
        ).orElseThrow()

        return ParameterUtils.readValue(quorumProperty.value) as Int
    }
}