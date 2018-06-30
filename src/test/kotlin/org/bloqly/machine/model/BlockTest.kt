package org.bloqly.machine.model

import org.bloqly.machine.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BlockTest {

    @Test
    fun testSyncBlockToVO() {

        val syncBlock = Block(
            id = "syncBlockId",
            spaceId = "main",
            height = 1,
            round = TimeUtils.getCurrentRound(),
            timestamp = Instant.now().toEpochMilli(),
            parentId = "lastBlockId",
            proposerId = "syncBlockId"
        )

        assertEquals(syncBlock, syncBlock.toVO().toModel())
    }
}