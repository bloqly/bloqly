package org.bloqly.machine.service

import org.bloqly.machine.model.Space
import org.bloqly.machine.repository.SpaceRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SpaceService(private val spaceRepository: SpaceRepository) {

    @Cacheable("spaces")
    @Transactional(readOnly = true)
    fun findAll(): Iterable<Space> =
        spaceRepository.findAll()

    @Cacheable("space")
    @Transactional(readOnly = true)
    fun findById(spaceId: String): Space? =
        spaceRepository.findById(spaceId).orElse(null)

    @Cacheable("space")
    @Transactional(readOnly = true)
    fun getById(spaceId: String): Space =
        spaceRepository.findById(spaceId).orElseThrow()

    @Cacheable("spaceExists")
    @Transactional(readOnly = true)
    fun existsById(spaceId: String): Boolean =
        spaceRepository.existsById(spaceId)

    @Cacheable("spaceIds")
    @Transactional(readOnly = true)
    fun getSpaceIds(): List<String> =
        spaceRepository.findAll().map { it.id }

    @CacheEvict(cacheNames = ["space", "spaces", "spaceExists", "spaceIds"], allEntries = true)
    @Transactional
    fun save(space: Space): Space =
        spaceRepository.save(space)
}