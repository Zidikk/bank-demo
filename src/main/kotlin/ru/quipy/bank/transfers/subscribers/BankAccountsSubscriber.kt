package ru.quipy.bank.transfers.subscribers

import org.springframework.stereotype.Component
import ru.quipy.bank.accounts.api.*
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.bank.transfers.api.TransactionConfirmedEvent
import ru.quipy.bank.transfers.api.TransactionFailedEvent
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
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>,
    private val sagaManager: SagaManager,
) {

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(AccountAggregate::class, "transactions::bank-accounts-subscriber") {
//            `when`(TransferTransactionAcceptedEvent::class) { event ->
//                val sagaContext = sagaManager
//                    .withContextGiven(event.sagaContext)
//                    .performSagaStep("TRANSACTION_SAGA", "participant acceptance").sagaContext
//
//                transactionEsService.update(event.transactionId) {
//                    it.processParticipantAccept(event.bankAccountId)
//                }
//            }
//            `when`(TransferTransactionDeclinedEvent::class) { event ->
//                val sagaContext = sagaManager
//                    .withContextGiven(event.sagaContext)
//                    .performSagaStep("TRANSACTION_SAGA", "participant decline").sagaContext
//
//                transactionEsService.update(event.transactionId) {
//                    it.processParticipantDecline(event.bankAccountId)
//                }
//            }
//            `when`(TransferTransactionProcessedEvent::class) { event ->
//                val sagaContext = sagaManager
//                    .withContextGiven(event.sagaContext)
//                    .performSagaStep("TRANSACTION_SAGA", "participant commit").sagaContext
//
//                transactionEsService.update(event.transactionId) {
//                    it.participantCommitted(event.bankAccountId)
//                }
//            }
//            `when`(TransferTransactionRollbackedEvent::class) { event ->
//                val sagaContext = sagaManager
//                    .withContextGiven(event.sagaContext)
//                    .performSagaStep("TRANSACTION_SAGA", "participant rollback").sagaContext
//
//                transactionEsService.update(event.transactionId) {
//                    it.participantRollbacked(event.bankAccountId)
//                }
//            }

            `when`(InternalAccountReceiveEvent::class) {event ->
                sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_SAGA", "finish transaction").sagaContext

                transactionEsService.update(event.transactionId){
                    it.finishTransaction()
                }
            }

            `when`(TransactionCancelEvent::class){event ->
                sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_SAGA", "cancel").sagaContext

                transactionEsService.update(event.transactionId){
                    it.cancelTransaction()
                }
            }

            `when`(TransferTransactionDeclinedEvent::class){event ->
                sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_SAGA", "cancel").sagaContext

                transactionEsService.update(event.transactionId){
                    it.cancelTransaction()
                }
            }

            `when`(InternalAccountSendEvent::class) { event ->
                sagaManager
                    .withContextGiven(event.sagaContext)
                    .performSagaStep("TRANSACTION_SAGA", "receive").sagaContext

                val outcomeTransaction = accountEsService.update(event.accountId) {
                    it.receiveTransaction(
                        event.bankAccountIdFrom,
                        event.accountId,
                        event.bankAccountIdTo,
                        event.transactionId,
                        event.amount
                    )
                }
            }


        }
    }
}