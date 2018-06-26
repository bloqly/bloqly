package org.bloqly.machine.simulation

data class Node(
    val id: Int,
    var lastBlock: Block,
    val proposals: MutableSet<Block> = mutableSetOf(),
    val votes: MutableSet<Vote> = mutableSetOf()
) {

    fun receiveVote(vote: Vote) {
        if (!SimUtils.isNetworkFailure()) {
            addVote(vote)
        }
    }

    private fun addVote(vote: Vote) {
        votes.add(vote)
    }

    fun receiveProposal(proposal: Block) {
        if (!SimUtils.isNetworkFailure()) {
            proposals.add(proposal)
        }
    }

    fun getProposal(): Block {
        val newHeight = lastBlock.height + 1

        // Did I propose new height already?
        //
        // What we do here is probably send a proposal in behalf of another validator
        // as his proposal is better. As we do it in our round, in real implementation
        // 'block' will be encapsulated in a BlockContainer with the required additional information
        // provided...
        val bestProposal = proposals.filter { it.height == newHeight }
            .sortedBy { it.round }
            .firstOrNull()

        return if (bestProposal != null) {
            bestProposal
        } else {
            val newProposal = Block(
                proposerId = id,
                height = newHeight,
                round = SimUtils.round
            )

            proposals.add(newProposal)

            newProposal
        }
    }

    fun getVote(): Vote {
        val newHeight = lastBlock.height + 1

        val savedVote = votes.firstOrNull {
            it.block.height == newHeight && it.block.proposerId == id
        }

        if (savedVote != null) {
            return savedVote
        }

        val proposal = proposals
            .filter { it.height == newHeight }
            .sortedBy { it.round }
            .firstOrNull()

        val newVote = if (proposal != null) {
            // proposal found, vote for it
            Vote(nodeId = id, block = proposal)
        } else {
            // no proposals found, vote for the last block
            Vote(nodeId = id, block = lastBlock)
        }

        votes.add(newVote)

        return newVote
    }

    fun tick() {
        val newHeight = lastBlock.height + 1

        val nextVotes = votes.filter { it.block.height == newHeight }.toSet()

        val nextProposals = nextVotes.map { it.block }.toSet()

        // Are we in deadlock now?
        if (nextVotes.size >= Nodes.quorum && nextProposals.size > Nodes.nodesCount - Nodes.quorum + 1) {
            println("Deadlock on node $id")

            // set the proof of deadlock as the next block

            val lockBlock = Block(
                parent = lastBlock,
                round = SimUtils.round,
                height = newHeight,
                proofOfLock = nextVotes
            )

            lastBlock = lockBlock
        } else {
            val bestProposal = proposals
                .filter { it.height == newHeight }
                .sortedBy { it.round }
                .firstOrNull {
                    val votesCount = votes.filter { vote -> vote.block == it }.size
                    votesCount >= Nodes.quorum
                }

            bestProposal?.let {
                lastBlock = bestProposal.copy(parent = lastBlock)
            }
        }
    }

    fun syncBlocks() {
        val height = lastBlock.height

        if (votes.isEmpty()) {
            return
        }

        val otherHeight = votes.map { it.block.height }.max()!!.toInt() - 1

        val nodeToAsk = Nodes.nodes[SimUtils.getRandomIndex()]

        if (nodeToAsk.id == id) {
            return
        }

        if (otherHeight > height) {
            val blocks = nodeToAsk.getBlocks(height - 1)

            proposals.addAll(blocks)
            nodeToAsk.votes.forEach { addVote(it) }
        }
    }

    fun syncVotes() {
        val nodeToAsk = Nodes.nodes[SimUtils.getRandomIndex()]

        if (nodeToAsk.id == id) {
            return
        }

        nodeToAsk.votes.forEach { addVote(it) }
    }

    private fun getBlocks(fromHeight: Long): Set<Block> {

        return getBlocks(fromHeight, lastBlock, setOf())
    }

    private tailrec fun getBlocks(fromHeight: Long, block: Block?, blocks: Set<Block>): Set<Block> {
        return if (block == null || block.height == fromHeight) {
            blocks
        } else {
            getBlocks(fromHeight, block.parent, blocks.plus(block))
        }
    }
}