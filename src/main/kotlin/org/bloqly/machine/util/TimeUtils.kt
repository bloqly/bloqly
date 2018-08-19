package org.bloqly.machine.util

import org.bloqly.machine.Application.Companion.ROUND
import java.time.Instant

object TimeUtils {

    private var TEST_TIME: Long? = null

    fun getCurrentRound(): Long = getCurrentTime() / ROUND

    fun getCurrentTime(): Long = TEST_TIME ?: Instant.now().toEpochMilli()

    fun setTestTime(time: Long) {
        TEST_TIME = time
    }

    fun reset() {
        TEST_TIME = null
    }

    fun setTestRound(round: Long) {
        setTestTime(round * ROUND + 1)
    }
}