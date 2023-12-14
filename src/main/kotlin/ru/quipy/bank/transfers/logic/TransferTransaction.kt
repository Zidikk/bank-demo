package ru.quipy.bank.transfers.logic

import ru.quipy.bank.transfers.api.*
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.*

import ru.quipy.bank.transfers.logic.TransferTransaction.TransactionState.*
import java.math.BigDecimal

class TransferTransaction : AggregateState<UUID, TransferTransactionAggregate> {
    private lateinit var transferId: UUID
    internal var transactionState = CREATED

    private lateinit var sourceParticipant: Participant
    private lateinit var destinationParticipant: Participant

    private lateinit var transferAmount: BigDecimal

    override fun getId() = transferId

    fun initiateTransferTransaction(
        id: UUID = UUID.randomUUID(),
        sourceAccountId: UUID,
        sourceBankAccountId: UUID,
        destinationAccountId: UUID,
        destinationBankAccountId: UUID,
        transferAmount: BigDecimal
    ): TransferTransactionCreatedEvent {
        // todo sukhoa validation
        return TransferTransactionCreatedEvent(
            id,
            sourceAccountId,
            sourceBankAccountId,
            destinationAccountId,
            destinationBankAccountId,
            transferAmount
        )
    }



    fun finishTransaction() : TransactionSucceededEvent{
        return TransactionSucceededEvent(transferId)
    }

    fun cancelTransaction() : TransactionFailedEvent{
       return TransactionFailedEvent(transferId)
    }


    @StateTransitionFunc
    fun initiateTransferTransaction(event: TransferTransactionCreatedEvent) {
        transactionState = CREATED
        this.transferId = event.transferId
        this.sourceParticipant = Participant(event.sourceAccountId, event.sourceBankAccountId)
        this.destinationParticipant = Participant(event.destinationAccountId, event.destinationBankAccountId)
        this.transferAmount = event.transferAmount
    }


    @StateTransitionFunc
    fun succeeded(event: TransactionSucceededEvent) {
        transactionState = SUCCEEDED
    }

    @StateTransitionFunc
    fun failed(event: TransactionFailedEvent) {
        transactionState = CANCELLED
    }


    @StateTransitionFunc
    fun noop(event: NoopEvent) = Unit

    data class Participant(
        internal val accountId: UUID,
        internal val bankAccountId: UUID,
    )

    enum class TransactionState {
        CREATED,
        CANCELLED,
        SUCCEEDED
    }
}
