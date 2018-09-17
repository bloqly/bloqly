package org.bloqly.machine.util

object APIUtils {

    fun getDataPath(nodeId: String, noun: String): String {
        return "http://$nodeId/api/v1/data/$noun"
    }

    fun getEventPath(nodeId: String, noun: String): String {
        return "http://$nodeId/api/v1/data/event/$noun"
    }
}