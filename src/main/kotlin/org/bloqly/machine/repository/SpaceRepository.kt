package org.bloqly.machine.repository

import org.bloqly.machine.model.Space
import org.springframework.data.repository.CrudRepository

interface SpaceRepository : CrudRepository<Space, String>