package org.bloqly.machine.controller

import org.bloqly.machine.component.EventReceiverService
import org.bloqly.machine.vo.VoteList
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("server")
@RestController
@RequestMapping("/data/votes")
class VoteController(
    private val eventReceiverService: EventReceiverService
) {
    @PostMapping
    fun onVotes(@RequestBody votesList: VoteList) {
        eventReceiverService.receiveVotes(votesList.votes)
    }
}