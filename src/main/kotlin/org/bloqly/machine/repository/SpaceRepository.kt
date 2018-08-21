package org.bloqly.machine.repository

import org.bloqly.machine.model.Space
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface SpaceRepository : CrudRepository<Space, String> {

    @Cacheable("spaceExists")
    override fun existsById(spaceId: String): Boolean

    @Cacheable("spaces")
    override fun findAll(): List<Space>

    @Cacheable("space")
    override fun findById(id: String?): Optional<Space>

    @CacheEvict(
        value = [
            "space",
            "spaces",
            "spaceExists"
        ]
    )
    override fun <S : Space?> save(space: S): S
}