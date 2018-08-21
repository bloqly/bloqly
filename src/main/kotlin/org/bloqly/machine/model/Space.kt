package org.bloqly.machine.model

import org.apache.commons.lang3.builder.EqualsBuilder
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Space(

    @Id
    val id: String,

    @Column(nullable = false)
    val creatorId: String

) {
    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other == this) {
            return true
        }
        if (other !is Space) {
            return false
        }
        return EqualsBuilder().append(this.id, other.id).build()
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}