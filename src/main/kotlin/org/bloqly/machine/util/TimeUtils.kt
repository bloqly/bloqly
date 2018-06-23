package org.bloqly.machine.util

import org.bloqly.machine.Application.Companion.PERIOD
import java.time.Instant

object TimeUtils {

    private var TEST_TIME: Long? = null

    fun getCurrentRound(): Long {
        return TEST_TIME ?: Instant.now().toEpochMilli() / PERIOD
    }

    fun setTestTime(time: Long) {
        TEST_TIME = time
    }

    fun reset() {
        TEST_TIME = null
    }
}