package org.bloqly.machine.repository

import org.bloqly.machine.model.Block
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface BlockRepository : CrudRepository<Block, String> {

    fun existsBySpace(space: String): Boolean

    fun findFirstBySpaceOrderByHeightDesc(space: String): Block

    @Query("select b from Block b where b.height = 0 and space = ?1")
    fun findGenesisBlockBySpace(space: String): Block

    @Query("select b from Block b where b.space = ?1 and b.height >= ?2 and b.height < ?3")
    fun getBlocksDelta(space: String, startHeight: Long, endHeight: Long): List<Block>
}