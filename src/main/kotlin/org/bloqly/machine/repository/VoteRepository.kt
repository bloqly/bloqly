package org.bloqly.machine.repository

import org.bloqly.machine.model.Account
import org.bloqly.machine.model.Vote
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, Long> {

    fun findByBlockHash(blockId: String): List<Vote>

    @Query("select * from vote where space_id = ?1 order by height desc limit 1", nativeQuery = true)
    fun findLastForSpace(spaceId: String): Vote

    fun findByValidatorAndBlockHash(validator: Account, blockHash: String): Vote?

    fun existsBySpaceIdAndValidatorAndHeight(spaceId: String, validator: Account, height: Long): Boolean

    fun existsByValidatorAndBlockHash(validator: Account, blockHash: String): Boolean
}