package org.bloqly.machine.repository

import org.bloqly.machine.model.EntityEvent
import org.bloqly.machine.model.EntityEventId
import org.springframework.data.repository.CrudRepository

interface EntityEventRepository : CrudRepository<EntityEvent, EntityEventId>