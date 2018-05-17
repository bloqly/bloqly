package org.bloqly.machine.repository

import org.bloqly.machine.model.Vote
import org.bloqly.machine.model.VoteId
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, VoteId> {

    fun findByBlockId(blockId: String): List<Vote>

}