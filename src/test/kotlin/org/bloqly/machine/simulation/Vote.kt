package org.bloqly.machine.simulation

data class Vote(
    val nodeId: Int,
    val block: Block,
    val voteType: VoteType = VoteType.VOTE,
    val voteRound: Int = SimUtils.round
)