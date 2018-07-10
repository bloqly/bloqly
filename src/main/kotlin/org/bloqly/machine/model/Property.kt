package org.bloqly.machine.model

import org.bloqly.machine.util.ParameterUtils
import java.util.Arrays
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Lob

@Entity
data class Property(

    @EmbeddedId
    val id: PropertyId,

    @Lob
    @Column(nullable = false)
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

    override fun toString(): String {
        return "Property(id=$id, value=${ParameterUtils.readValue(value)})"
    }
}
