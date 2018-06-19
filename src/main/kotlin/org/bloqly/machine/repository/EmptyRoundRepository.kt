package org.bloqly.machine.repository

import org.bloqly.machine.model.EmptyRound
import org.bloqly.machine.model.RoundId
import org.springframework.data.repository.CrudRepository

interface EmptyRoundRepository : CrudRepository<EmptyRound, RoundId>