package org.bloqly.machine.repository

interface EmptyRoundRepositoryCustom {
    fun processEmptyRound(spaceId: String, height: Long)
}