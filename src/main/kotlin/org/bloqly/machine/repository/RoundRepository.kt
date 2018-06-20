package org.bloqly.machine.repository

import org.bloqly.machine.model.Round
import org.bloqly.machine.model.RoundId
import org.springframework.data.repository.CrudRepository

interface RoundRepository : CrudRepository<Round, RoundId>, RoundRepositoryCustom