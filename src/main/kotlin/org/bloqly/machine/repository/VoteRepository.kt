package org.bloqly.machine.repository

import org.bloqly.machine.model.Vote
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VoteRepository : CrudRepository<Vote, Long> {

    fun findByBlockHash(blockId: String): List<Vote>

    @Query("select * from vote where space_id = ?1 order by height desc limit 1", nativeQuery = true)
    fun findLastForSpace(spaceId: String): Vote?

    fun findByPublicKeyAndBlockHash(publicKey: String, blockHash: String): Vote?

    fun existsBySpaceIdAndPublicKeyAndHeight(spaceId: String, publicKey: String, height: Long): Boolean

    @Query("select count(v) > 0 from Vote v where v.spaceId = ?1 and v.publicKey = ?2 and v.height = ?3")
    fun existsByHeight(spaceId: String, publicKey: String, height: Long): Boolean

    fun existsByPublicKeyAndBlockHash(publicKey: String, blockHash: String): Boolean

    @Query(
        """
        select * from vote v
        where
        v.space_id = ?1 and
        v.public_key = ?2
        order by height desc limit 1
        """, nativeQuery = true
    )
    fun findBestVote(spaceId: String, publicKey: String): Vote?
}