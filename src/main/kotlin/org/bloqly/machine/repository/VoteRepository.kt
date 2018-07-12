package org.bloqly.machine.repository

import org.bloqly.machine.model.Vote
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, Long> {

    fun findByBlockHash(blockId: String): List<Vote>

    @Query("select * from vote where space_id = ?1 order by height desc limit 1", nativeQuery = true)
    fun findLastForSpace(spaceId: String): Vote

    @Query(
        """
        select v.* from vote v, block b
        where
        b.hash = v.block_hash and
        b.space_id = ?1 and
        v.validator_id = ?2 and
        v.height = ?3""", nativeQuery = true
    )
    fun findOwnVote(spaceId: String, validatorId: String, height: Long): Vote?

    fun existsByValidatorIdAndSpaceIdAndHeight(
        validatorId: String, spaceId: String, height: Long
    ): Boolean
}