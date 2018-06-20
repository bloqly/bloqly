package org.bloqly.machine.util

import org.bloqly.machine.Application.Companion.PERIOD
import java.time.Instant

object TimeUtils {

    fun getCurrentRound(): Long {
        return Instant.now().toEpochMilli() / PERIOD
    }
}