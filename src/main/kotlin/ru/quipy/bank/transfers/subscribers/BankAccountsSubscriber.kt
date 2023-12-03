package ru.quipy.bank.transfers.subscribers

import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.*
import ru.quipy.bank.transfers.api.TransferTransactionAggregate
import ru.quipy.bank.transfers.logic.TransferTransaction
import ru.quipy.core.EventSourcingService
import ru.quipy.saga.SagaManager
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct


@Component
class BankAccountsSubscriber(
    private val subscriptionsManager: AggregateSubscriptionsManager,
    private val transactionEsService: EventSourcingService<UUID, TransferTransactionAggregate, TransferTransaction>,
    private val sagaManager: SagaManager
) {

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "transactions::bank-accounts-subscriber") {
            `when`(TransferTransactionAcceptedEvent::class) { event ->
                val sagaContext = sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_PROCESSING", "participant acceptance").sagaContext

                transactionEsService.update(event.transactionId) {
                    it.processParticipantAccept(event.bankAccountId)
                }
            }
            `when`(TransferTransactionDeclinedEvent::class) { event ->
                val sagaContext = sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_PROCESSING", "participant decline").sagaContext

                transactionEsService.update(event.transactionId) {
                    it.processParticipantDecline(event.bankAccountId)
                }
            }
            `when`(TransferTransactionProcessedEvent::class) { event ->
                val sagaContext = sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_PROCESSING", "participant commit").sagaContext

                transactionEsService.update(event.transactionId) {
                    it.participantCommitted(event.bankAccountId)
                }
            }
            `when`(TransferTransactionRollbackedEvent::class) { event ->
                val sagaContext = sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_PROCESSING", "participant rollback").sagaContext

                transactionEsService.update(event.transactionId) {
                    it.participantRollbacked(event.bankAccountId)
                }
            }
        }
    }
}