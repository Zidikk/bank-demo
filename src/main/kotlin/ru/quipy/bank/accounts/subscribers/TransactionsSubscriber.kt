package ru.quipy.bank.accounts.subscribers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.InternalAccountSendEvent
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.transfers.api.TransferTransactionAggregate
import ru.quipy.bank.transfers.api.TransferTransactionCreatedEvent
import ru.quipy.core.EventSourcingService
import ru.quipy.saga.SagaManager
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct

@Component
class TransactionsSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>,
    private val sagaManager: SagaManager
) {
    private val logger: Logger = LoggerFactory.getLogger(TransactionsSubscriber::class.java)

        @PostConstruct
        fun init() {
            subscriptionsManager.createSubscriber(
                TransferTransactionAggregate::class,
                "accounts::transaction-processing-subscriber"
            ) {
                `when`(TransferTransactionCreatedEvent::class) { event ->
                    logger.info("Got transaction to process: $event")

                    val sagaContext = sagaManager
                        .withContextGiven(event.sagaContext)
                        .performSagaStep("TRANSACTION_SAGA", "outcome transaction").sagaContext

                    val outcomeTransaction = accountEsService.update(event.sourceAccountId) {
                        it.sendTransaction(
                            event.sourceBankAccountId,
                            event.destinationBankAccountId,
                            event.transferId,
                            event.transferAmount
                        )
                    }
                }
            }
        }
}