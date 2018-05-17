package org.bloqly.machine.service

import org.bloqly.machine.model.Contract

data class ContractInvocationContext(val functionName: String,
                                     val caller: String,
                                     val callee: String,
                                     val contract: Contract)
