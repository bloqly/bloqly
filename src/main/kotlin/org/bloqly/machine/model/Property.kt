package org.bloqly.machine.model

import java.util.Arrays
import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
data class Property(

    @EmbeddedId
    val id: PropertyId,

    val value: ByteArray

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Property

        if (id != other.id) return false
        if (!Arrays.equals(value, other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + Arrays.hashCode(value)
        return result
    }
}
