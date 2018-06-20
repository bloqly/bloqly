package org.bloqly.machine.repository

import org.bloqly.machine.model.Round
import org.bloqly.machine.model.RoundId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface RoundRepository : CrudRepository<Round, RoundId>, RoundRepositoryCustom {

    @Query("select round from round r where r.space_id = ?1 order by round desc limit 1", nativeQuery = true)
    fun getRoundValue(space: String): Long

    @Query("select * from round r where r.space_id = ?1 order by round desc limit 1", nativeQuery = true)
    fun getRound(space: String): Round
}