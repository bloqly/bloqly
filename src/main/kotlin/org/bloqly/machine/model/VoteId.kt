package org.bloqly.machine.model

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class VoteId(

    @Column(nullable = false)
    val validatorId: String,

    @Column(nullable = false)
    val space: String,

    @Column(nullable = false)
    val height: Long

) : Serializable