package org.bloqly.machine.model

import org.bloqly.machine.model.Contract

data class ContractInvocationContext(val functionName: String,
                                     val caller: String,
                                     val callee: String,
                                     val contract: Contract)
