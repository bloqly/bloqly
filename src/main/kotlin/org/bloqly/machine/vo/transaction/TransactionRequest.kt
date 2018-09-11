package org.bloqly.machine.vo.transaction

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.bloqly.machine.Application.Companion.DEFAULT_FUNCTION
import org.bloqly.machine.Application.Companion.DEFAULT_SELF
import org.bloqly.machine.Application.Companion.DEFAULT_SPACE
import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.TransactionType
import org.bloqly.machine.vo.property.Value

@ApiModel
@ValueObject
data class TransactionRequest(
    @ApiModelProperty(value = "Space id, default value is 'main'", example = "main")
    var space: String = DEFAULT_SPACE,

    @ApiModelProperty("Transaction origin")
    val origin: String,

    @ApiModelProperty("Transaction issuer passphrase")
    val passphrase: String,

    @ApiModelProperty("Transaction destination")
    val destination: String,

    @ApiModelProperty("Transaction type", example = "CALL")
    var transactionType: String = TransactionType.CALL.name,

    @ApiModelProperty(value = "Contract address which will process this transaction", example = "self")
    var self: String = DEFAULT_SELF,

    @ApiModelProperty(value = "Contract method name which will process this transaction", example = "main")
    var key: String = DEFAULT_FUNCTION,

    @ApiModelProperty("List of transaction arguments")
    val args: List<Value>
)