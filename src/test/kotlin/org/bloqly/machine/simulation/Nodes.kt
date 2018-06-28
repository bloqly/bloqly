package org.bloqly.machine.simulation

object Nodes {

    const val quorum = 15
    const val nodesCount: Int = 21

    const val maxRoundDelay = 5

    val nodes: List<Node>

    init {
        val block = Block(height = 0, round = 0)

        nodes = (0 until nodesCount)
            .map { index -> Node(id = index, lastBlock = block) }
            .sortedBy { it.id }

        nodes[0].faulty = true
        nodes[1].faulty = true
    }

    fun getProposer(): Node? {
        val round = SimUtils.round

        val proposerIndex = round % nodesCount

        return nodes[proposerIndex]
    }
}