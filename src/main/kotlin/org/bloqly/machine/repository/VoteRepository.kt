package org.bloqly.machine.repository

import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, VoteId> {

    fun findByBlockId(blockId: String): List<Vote>

    @Query("select * from vote where space_id = ?1 order by height limit 1", nativeQuery = true)
    fun findLastForSpace(spaceId: String): Vote

    @Query("select * from vote where height = ?1", nativeQuery = true)
    fun findByHeight(height: Long): List<Vote>

    @Query(
        """
        select count(*) from vote
        where
        space_id = ?1 and
        height = ?2 and
        vote_type = 'PRE_LOCK'
        """, nativeQuery = true
    )
    fun findPreLocksCountByHeight(spaceId: String, height: Long): Int

    @Query(
        """
        select count(*) from vote
        where
        space_id = ?1 and
        height = ?2 and
        vote_type = 'LOCK'
        """, nativeQuery = true
    )
    fun findLocksCountByHeight(spaceId: String, height: Long): Int
}