package org.bloqly.machine.repository

import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, VoteId> {

    fun findByBlockId(blockId: String): List<Vote>

    @Query("select * from vote where space_id = ?1 order by height limit 1", nativeQuery = true)
    fun findLastForSpace(spaceId: String): Vote
}