package org.bloqly.machine.repository

import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, VoteId> {

    fun findByBlockId(blockId: String): List<Vote>

    @Query("select * from vote where space_id = ?1 order by height desc limit 1", nativeQuery = true)
    fun findLastForSpace(spaceId: String): Vote

    @Query(
        """
        select count(*) from vote
        where
        space_id = ?1 and
        height = ?2 and
        vote_type = 'PRE_SYNC'
        """, nativeQuery = true
    )
    fun findPreSyncCountByHeight(spaceId: String, height: Long): Int

    @Query(
        """
        select count(*) from vote
        where
        space_id = ?1 and
        height = ?2 and
        vote_type = 'SYNC'
        """, nativeQuery = true
    )
    fun findSyncCountByHeight(spaceId: String, height: Long): Int

    @Query(
        """
        select  * from vote
        where
        height = ?1 and
        validator_id = ?2
        """, nativeQuery = true
    )
    fun findByHeightAndValidatorId(height: Long, validatorId: String): List<Vote>
}