package org.bloqly.machine.model

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.bloqly.machine.vo.property.Value
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
@TypeDefs(TypeDef(name = "jsonb", typeClass = JsonBinaryType::class))
data class Property(

    @EmbeddedId
    val id: PropertyId,

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", nullable = false)
    val value: Value
) {
    fun toValue(): Any = value.toValue()
}