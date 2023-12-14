package ru.quipy.bank.accounts.service

import ru.quipy.bank.accounts.api.AccountAggregate
import ru.quipy.bank.accounts.api.InternalAccountSendEvent
import ru.quipy.bank.accounts.logic.Account
import ru.quipy.core.EventSourcingService
import ru.quipy.saga.SagaManager
import java.util.*

class AccountService(
    private val accountEsService: EventSourcingService<UUID, AccountAggregate, Account>,
    private val sagaManager: SagaManager
) {
    fun receiveMoney(event: InternalAccountSendEvent){
        val sagaContext = sagaManager
            .withContextGiven(event.sagaContext)
            .performSagaStep("TRANSACTION_SAGA","receive").sagaContext

        val outcomeTransaction = accountEsService.update(event.accountId){
            it.receiveTransaction(
                event.bankAccountIdFrom,
                event.accountId,
                event.bankAccountIdTo,
                event.transactionId,
                event.amount)
        }
    }

}