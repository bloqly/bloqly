package org.bloqly.machine.vo.property

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.lang.BLong
import org.bloqly.machine.model.ValueType
import org.bloqly.machine.util.decode16
import org.bloqly.machine.util.encode16
import java.io.Serializable
import java.math.BigInteger

@ApiModel("Value type")
@ValueObject
data class Value(

    @ApiModelProperty(value = "Type", allowableValues = "STRING, INT, BIGINT, BOOLEAN", example = "INT")
    val type: ValueType,

    @ApiModelProperty(value = "Value", example = "100")
    val value: String
) : Serializable {
    fun toValue(): Any {
        return when (type) {
            ValueType.BIGINT -> BLong(value)
            ValueType.INT -> value.toLong()
            ValueType.BOOLEAN -> value.toBoolean()
            ValueType.STRING -> String(value.decode16())
        }
    }

    companion object {

        fun of(vararg args: Any): List<Value> = args.map { Value.of(it) }

        fun ofs(input: Any): List<Value> = listOf(of(input))

        fun ofArray(args: Array<Any>): List<Value> = args.map { Value.of(it) }

        fun of(input: Any): Value =
            when (input) {
                is String -> Value(ValueType.STRING, input.toByteArray().encode16())
                is Long -> Value(ValueType.INT, input.toString())
                is Int -> Value(ValueType.INT, input.toString())
                is BLong -> Value(ValueType.BIGINT, input.toString())
                is BigInteger -> Value(ValueType.BIGINT, input.toString())
                is Boolean -> Value(ValueType.BOOLEAN, input.toString())
                else -> throw IllegalArgumentException(
                    "Unsupported input $input of type ${input.javaClass.canonicalName}"
                )
            }
    }
}