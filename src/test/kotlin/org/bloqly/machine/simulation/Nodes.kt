package org.bloqly.machine.simulation

object Nodes {

    const val quorum = 3
    const val nodesCount: Int = 4
    const val maxRoundDelay = 10

    val nodes: List<Node>

    init {
        val block = Block(height = 0, round = 0)

        nodes = (0 until nodesCount)
            .map { index -> Node(id = index, lastBlock = block) }
            .sortedBy { it.id }

        nodes[0].faulty = true
    }

    fun getProposer(): Node? {
        val round = SimUtils.round

        val proposerIndex = round % nodesCount

        return nodes[proposerIndex]
    }
}