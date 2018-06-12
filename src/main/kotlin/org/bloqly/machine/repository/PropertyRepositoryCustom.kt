package org.bloqly.machine.repository

interface PropertyRepositoryCustom {
    fun getQuorum(space: String): Int
}