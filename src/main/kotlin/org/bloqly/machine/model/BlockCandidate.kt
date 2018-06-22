package org.bloqly.machine.model

import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Lob

@Entity
data class BlockCandidate(

    @EmbeddedId
    val id: BlockCandidateId,

    @Lob
    @Column(nullable = false)
    val data: String,

    @Column(nullable = false)
    val blockId: String,

    @Column(nullable = false)
    val timeReceived: Long

)