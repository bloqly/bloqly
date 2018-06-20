package org.bloqly.machine.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Space(

    @Id
    val id: String,

    @Column(nullable = false)
    val creatorId: String

)