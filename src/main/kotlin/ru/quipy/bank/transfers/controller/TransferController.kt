package ru.quipy.bank.transfers.controller;

import org.springframework.web.bind.annotation.*;
import ru.quipy.bank.transfers.api.TransferTransactionAggregate;
import ru.quipy.bank.transfers.api.TransferTransactionCreatedEvent
import ru.quipy.bank.transfers.logic.TransferTransaction;
import ru.quipy.bank.transfers.service.TransactionService
import ru.quipy.core.EventSourcingService;
import ru.quipy.saga.SagaManager;
import java.math.BigDecimal

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
class TransferController(
        val transactionService : TransactionService,
        val sagaManager : SagaManager
) {

    //        sourceAccountId: UUID,
    //        sourceBankAccountId: UUID,
    //        destinationAccountId: UUID,
    //        destinationBankAccountId: UUID,
    //        transferAmount: BigDecimal
    @PostMapping("/transfer/{senderAccountId}")
    fun moneyTransaction(
        @RequestParam sourceBankAccountId : UUID,
        @RequestParam destinationBankAccountId : UUID,
        @RequestParam transferAmmount : BigDecimal) : TransferTransactionCreatedEvent{
        val sagaContext = sagaManager
            .launchSaga("TRANSACTION_SAGA", "initiateTransaction")
            .sagaContext

        return transactionService.initiateTransferTransaction(sourceBankAccountId, destinationBankAccountId, transferAmmount)

    }
}
