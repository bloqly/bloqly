package org.bloqly.machine.component

import org.bloqly.machine.repository.SpaceRepository
import org.bloqly.machine.service.AccountService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LogService(
    private val spaceRepository: SpaceRepository,
    private val accountService: AccountService
) {

    private val log = LoggerFactory.getLogger(LogService::class.simpleName)

    /*
    @PostConstruct
    fun init() {
        spaceRepository.findAll().forEach { spaceId ->

            val validators = accountService.getValidatorsForSpace(spaceId)

            if (validators.isNotEmpty()) {
                log.info("validators for spaceId $spaceId:")
                validators.forEach { account ->
                    log.info("\t${account.id}")
                }
            }
        }
    }*/
}