package org.bloqly.machine.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Space(

        // space name
    @Id
    val id: String,

    @Column(nullable = false)
    val creatorId: String

)