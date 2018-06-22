package org.bloqly.machine.repository

import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.BlockCandidateId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface BlockCandidateRepository : CrudRepository<BlockCandidate, BlockCandidateId> {

    @Query("""
        select * from block_candidate bc
        where
        bc.space_id = ?1 and
        bc.height = ?2 and
        bc.proposer_id = ?3
        order by  bc.round
        limit 1
        """, nativeQuery = true)
    fun getBlockCandidate(spaceId: String, height: Long, proposerId: String): BlockCandidate?
}