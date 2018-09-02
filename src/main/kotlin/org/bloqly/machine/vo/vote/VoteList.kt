package org.bloqly.machine.vo.vote

import org.bloqly.machine.annotation.ValueObject
import org.bloqly.machine.model.Vote

@ValueObject
data class VoteList(
    val votes: List<VoteVO>
) {
    companion object {

        fun fromVotes(votes: List<Vote>): VoteList {
            return VoteList(votes.map { it.toVO() })
        }
    }
}