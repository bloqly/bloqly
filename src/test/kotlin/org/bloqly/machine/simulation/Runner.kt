package org.bloqly.machine.simulation

const val iterations = 10000

fun main(args: Array<String>) {

    var stopCounter = 0
    for (i in 0 until iterations) {

        println("ROUND: ${SimUtils.round}")

        val prevState = Nodes.nodes.map { it.copy() }

        SimUtils.nextRound()

        val proposer = Nodes.getProposer()

        proposer?.let {
            proposer.getProposal()
                ?.let { SimUtils.sendProposal(it) }
        }

        val votes = SimUtils.getVotes()

        SimUtils.sendVotes(votes)

        SimUtils.tick()

        SimUtils.sync()

        val state = Nodes.nodes.map { it.copy() }

        Nodes.nodes.forEach { node ->
            println("nodeId: ${node.id}, height: ${node.lastBlock.height}")
        }

        if (state == prevState) {
            stopCounter++
        } else {
            stopCounter = 0
        }

        if (stopCounter > Nodes.nodesCount * 5) {
            println("STOP")
            // break
        }

        println()
    }

    println("Locks count: ${SimUtils.locksCount}")
    println("Deadlocks count: ${SimUtils.locksCount}")

}