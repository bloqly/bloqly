package org.bloqly.machine.simulation

import java.util.Random

object SimUtils {

    private val random: Random = Random()

    init {
        random.setSeed(2)
    }

    var round: Int = 0

    fun getRandomIndex(): Int {
        return random.nextInt(Nodes.nodesCount)
    }

    fun nextRound() {
        round++
    }

    fun sendProposal(proposal: Block) {
        Nodes.nodes
            .forEach { node ->
                if (node.id != proposal.proposerId) {
                    node.receiveProposal(proposal)
                }
            }
    }

    fun getVotes(): Set<Vote> {
        return Nodes.nodes.map { it.getVote() }.toSet()
    }

    fun sendVotes(votes: Set<Vote>) {
        Nodes.nodes
            .forEach { node ->

                votes.forEach { vote ->
                    if (node.id != vote.nodeId) {
                        node.receiveVote(vote)
                    }
                }
            }
    }

    fun tick() {
        Nodes.nodes.forEach { it.tick() }
    }

    fun sync() {
        Nodes.nodes.forEach {
            it.syncBlocks()
            it.syncVotes()
        }
    }

    fun isNetworkFailure(): Boolean {
        return random.nextInt(10) < 1
    }
}