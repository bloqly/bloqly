package org.bloqly.machine.repository

import org.bloqly.machine.model.Block
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface BlockRepository : CrudRepository<Block, String> {

    fun existsBySpaceId(spaceId: String): Boolean

    // TODO  add sorting by block_id?
    @Query(
        """
        select * from block
        where
        space_id = ?1
        order by height desc, diff desc, weight desc, round asc
        limit 1
        """, nativeQuery = true
    )
    fun getLastBlock(spaceId: String): Block

    @Query("select b from Block b where b.height = 0 and spaceId = ?1")
    fun findGenesisBlockBySpaceId(spaceId: String): Block

    @Query("select b from Block b where b.spaceId = ?1 and b.height >= ?2 and b.height < ?3")
    fun getBlocksDelta(spaceId: String, startHeight: Long, endHeight: Long): List<Block>

    fun findBySpaceIdAndProposerIdAndRound(spaceId: String, proposerId: String, round: Long): Block?

    fun existsByHash(referencedBlockHash: String): Boolean

    fun findByHash(referencedBlockHash: String): Block?
}