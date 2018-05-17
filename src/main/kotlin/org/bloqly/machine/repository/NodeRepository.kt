package org.bloqly.machine.repository

import org.bloqly.machine.model.Node
import org.bloqly.machine.model.NodeId
import org.springframework.data.repository.CrudRepository

interface NodeRepository : CrudRepository<Node, NodeId>