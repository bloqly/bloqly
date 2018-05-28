package org.bloqly.machine.service

import org.bloqly.machine.repository.BlockRepository
import org.bloqly.machine.vo.GenesisVO
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class GenesisService(
    private val blockRepository: BlockRepository) {

    fun exportGenesis(space: String): GenesisVO {


        val genesisVO = GenesisVO()

        return genesisVO
    }
}