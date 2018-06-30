package org.bloqly.machine.model

import org.bloqly.machine.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BlockTest {

    @Test
    fun testLockBlockToVO() {

        val lockBlock = Block(
            id = "lockBlockId",
            spaceId = "main",
            height = 1,
            round = TimeUtils.getCurrentRound(),
            timestamp = Instant.now().toEpochMilli(),
            parentHash = "lastBlockId",
            proposerId = "lockBlockId"
        )

        assertEquals(lockBlock, lockBlock.toVO().toModel())
    }
}