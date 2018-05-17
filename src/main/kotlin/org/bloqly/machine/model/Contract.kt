package org.bloqly.machine.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Contract(

    @Id
    val id: String, // self

    @Column(nullable = false)
    val space: String,

    @Column(nullable = false)
    val owner: String? = null,

    @Column(columnDefinition = "text")
    val body: String
)
