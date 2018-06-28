package org.bloqly.machine.util

import org.bloqly.machine.Application.Companion.ROUND
import java.time.Instant

object TimeUtils {

    private var TEST_TIME: Long? = null

    fun getCurrentRound(): Long {
        return TEST_TIME ?: Instant.now().toEpochMilli() / ROUND
    }

    fun setTestTime(time: Long) {
        TEST_TIME = time
    }

    fun reset() {
        TEST_TIME = null
    }
}