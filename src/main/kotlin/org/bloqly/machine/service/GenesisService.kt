package org.bloqly.machine.service

import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class GenesisService (
    private val blockService: BlockService) {

    fun getGenesis() {

    }
}