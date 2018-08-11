package org.bloqly.machine.util

import org.bloqly.machine.model.Node

object APIUtils {

    fun getDataPath(node: Node, noun: String): String {
        return "http://${node.id}/api/v1/data/$noun"
    }

    fun getEventPath(node: Node, noun: String): String {
        return "http://${node.id}/api/v1/data/event/$noun"
    }
}