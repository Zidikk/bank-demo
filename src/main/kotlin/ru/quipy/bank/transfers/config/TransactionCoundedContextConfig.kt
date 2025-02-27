package ru.quipy.bank.transfers.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.quipy.bank.transfers.api.TransferTransactionAggregate
import ru.quipy.bank.transfers.logic.TransferTransaction
import ru.quipy.core.EventSourcingService
import ru.quipy.core.EventSourcingServiceFactory
import java.util.*

@Configuration
class TransactionCoundedContextConfig {

    @Autowired
    private lateinit var eventSourcingServiceFactory: EventSourcingServiceFactory

    @Bean
    fun transactionEsService(): EventSourcingService<UUID, TransferTransactionAggregate, TransferTransaction> =
        eventSourcingServiceFactory.create()

}