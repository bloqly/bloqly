package org.bloqly.machine.repository

import org.bloqly.machine.model.Block
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface BlockRepository : CrudRepository<Block, Long> {

    fun existsBySpaceId(spaceId: String): Boolean

    @Query(
        """
        select * from block
        where
        space_id = ?1
        order by height desc, diff desc, weight desc, round, hash
        limit 1
        """, nativeQuery = true
    )
    fun getLastBlock(spaceId: String): Block

    @Query("select b from Block b where b.height = 0 and spaceId = ?1")
    fun findGenesisBlockBySpaceId(spaceId: String): Block

    @Query("select b from Block b where b.spaceId = ?1 and b.height >= ?2 and b.height < ?3")
    fun getBlocksDelta(spaceId: String, startHeight: Long, endHeight: Long): List<Block>

    fun findBySpaceIdAndProducerIdAndRound(spaceId: String, producerId: String, round: Long): Block?

    fun existsByHash(referencedBlockHash: String): Boolean

    fun findByHash(hash: String): Block?

    fun findByLibHash(hash: String): Block?

    fun existsByHashAndLibHash(hash: String, libHash: String): Boolean

    fun existsByHashAndParentHash(hash: String, parentHash: String): Boolean

    fun existsBySpaceIdAndProducerIdAndHeight(spaceId: String, producerId: String, height: Long): Boolean

    fun existsBySpaceIdAndProducerIdAndRound(spaceId: String, producerId: String, round: Long): Boolean

    fun findByParentHash(parentHash: String): Block?
}