package org.bloqly.machine.repository

import org.bloqly.machine.model.BlockCandidate
import org.bloqly.machine.model.BlockCandidateId
import org.springframework.data.repository.CrudRepository

interface BlockCandidateRepository : CrudRepository<BlockCandidate, BlockCandidateId>