package org.bloqly.machine.repository

import org.bloqly.machine.model.Block
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface BlockRepository : CrudRepository<Block, String> {

    fun existsBySpaceId(spaceId: String): Boolean

    @Query("select * from block where space_id = ?1 order by height desc limit 1", nativeQuery = true)
    fun getLastBlock(spaceId: String): Block

    @Query("select b from Block b where b.height = 0 and spaceId = ?1")
    fun findGenesisBlockBySpaceId(spaceId: String): Block

    @Query("select b from Block b where b.spaceId = ?1 and b.height >= ?2 and b.height < ?3")
    fun getBlocksDelta(spaceId: String, startHeight: Long, endHeight: Long): List<Block>
}