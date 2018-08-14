package org.bloqly.machine.service

import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.SpaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation.SERIALIZABLE
import org.springframework.transaction.annotation.Transactional

@Service
class SpaceService(private val spaceRepository: SpaceRepository) {

    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun findAll(): Iterable<Space> =
        spaceRepository.findAll()

    @Transactional(isolation = SERIALIZABLE, readOnly = true)
    fun findById(spaceId: String): Space? =
        spaceRepository.findById(spaceId).orElse(null)
}