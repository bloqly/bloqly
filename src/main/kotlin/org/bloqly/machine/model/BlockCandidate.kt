package org.bloqly.machine.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Lob

@Entity
data class BlockCandidate(

    @EmbeddedId
    val id: BlockCandidateId,

    @Lob
    val data: String,

    val timeReceived: Long

)