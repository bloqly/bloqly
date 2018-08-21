package org.bloqly.machine.service

import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.SpaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SpaceService(private val spaceRepository: SpaceRepository) {

    @Transactional(readOnly = true)
    fun findAll(): Iterable<Space> =
        spaceRepository.findAll()

    @Transactional(readOnly = true)
    fun getById(spaceId: String): Space =
        spaceRepository.findById(spaceId).orElseThrow()

    @Transactional(readOnly = true)
    fun existsById(spaceId: String): Boolean =
        spaceRepository.existsById(spaceId)

    @Transactional(readOnly = true)
    fun getSpaceIds(): List<String> =
        spaceRepository.findAll().map { it.id }

    @Transactional
    fun save(space: Space): Space =
        spaceRepository.save(space)
}