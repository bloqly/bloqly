package org.bloqly.machine.model

enum class TransactionType {
    SET, // sets the data if key is not associated wit contract
    CALL, // expects contract to be created before with CALL
    CREATE, // creates contract
    DESTROY, // destroys contract
    UPDATE // updates contract, expects contract to be present
}
