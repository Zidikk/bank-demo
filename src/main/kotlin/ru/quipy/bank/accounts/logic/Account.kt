package ru.quipy.bank.accounts.logic

import ru.quipy.bank.accounts.api.*
import ru.quipy.bank.transfers.api.TransactionFailedEvent
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import ru.quipy.domain.Event
import java.math.BigDecimal
import java.util.*

// вопрос, что делать, если, скажем, обрабатываем какой-то ивент, понимаем, что агрегата, который нужно обновить не существует.
// Может ли ивент (ошибка) существовать в отрыве от агрегата?


class Account : AggregateState<UUID, AccountAggregate> {
    private lateinit var accountId: UUID
    private lateinit var holderId: UUID
    var bankAccounts: MutableMap<UUID, BankAccount> = mutableMapOf()

    override fun getId() = accountId

    fun createNewAccount(id: UUID = UUID.randomUUID(), holderId: UUID): AccountCreatedEvent {
        return AccountCreatedEvent(id, holderId)
    }

    fun createNewBankAccount(): BankAccountCreatedEvent {
        if (bankAccounts.size >= 5)
            throw IllegalStateException("Account $accountId already has ${bankAccounts.size} bank accounts")

        return BankAccountCreatedEvent(accountId = accountId, bankAccountId = UUID.randomUUID())
    }

    fun deposit(toBankAccountId: UUID, amount: BigDecimal): BankAccountDepositEvent {
        val bankAccount = (bankAccounts[toBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $toBankAccountId"))

        if (bankAccount.balance + amount > BigDecimal(10_000_000))
            throw IllegalStateException("You can't store more than 10.000.000 on account ${bankAccount.id}")

        if (bankAccounts.values.sumOf { it.balance } + amount > BigDecimal(25_000_000))
            throw IllegalStateException("You can't store more than 25.000.000 in total")


        return BankAccountDepositEvent(
            accountId = accountId,
            bankAccountId = toBankAccountId,
            amount = amount
        )
    }


    fun sendTransaction(
        fromBankAccountId: UUID,
        toBankAccountId: UUID,
        transactionId: UUID,
        transferAmount: BigDecimal
    ): Event<AccountAggregate> {
        val bankAccount = bankAccounts[fromBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer from: $fromBankAccountId")

        if (transferAmount > bankAccount.balance) {
            return TransactionCancelEvent(
                accountId = accountId,
                bankAccountId = fromBankAccountId,
                transactionId = transactionId,
                amount = transferAmount,
                "Cannot withdraw $transferAmount. Not enough money: ${bankAccount.balance}"

            )
        }


        return InternalAccountSendEvent(
            transactionId = transactionId,
            accountId = accountId,
            bankAccountIdFrom = fromBankAccountId,
            bankAccountIdTo = toBankAccountId,
            amount = transferAmount
        )

    }


    fun receiveTransaction(
        fromBankAccountId: UUID,
        fromAccountId : UUID,
        toBankAccountId: UUID,
        transactionId: UUID,
        transferAmount: BigDecimal
    ): Event<AccountAggregate> {
        val bankAccount = bankAccounts[toBankAccountId]
            ?: throw IllegalArgumentException("No such account to transfer to: $fromBankAccountId")

        if (bankAccount.balance + transferAmount > BigDecimal(10_000_000)) {
            return TransferTransactionDeclinedEvent(
                accountId = accountId,
                bankAccountId = toBankAccountId,
                transactionId = transactionId,
                transferAmount,
                "User can't store more than 10.000.000 on account: ${bankAccount.id}"
            )
        }

        if (bankAccounts.values.sumOf { it.balance } + transferAmount > BigDecimal(25_000_000)) {
            return TransferTransactionDeclinedEvent(
                accountId = accountId,
                bankAccountId = toBankAccountId,
                transactionId = transactionId,
                transferAmount,
                "User can't store more than 25.000.000 in total on account: ${bankAccount.id}"
            )
        }

        return InternalAccountReceiveEvent(
            transactionId = transactionId,
            fromAccountId = fromAccountId,
            bankAccountIdFrom = fromBankAccountId,
            toAccountId = accountId,
            bankAccountIdTo = toBankAccountId,
            amount = transferAmount
        )
    }

    fun withdraw(fromBankAccountId: UUID, amount: BigDecimal): BankAccountWithdrawalEvent {
        val fromBankAccount = bankAccounts[fromBankAccountId]
            ?: throw IllegalArgumentException("No such account to withdraw from: $fromBankAccountId")

        if (amount > fromBankAccount.balance) {
            throw IllegalArgumentException("Cannot withdraw $amount. Not enough money: ${fromBankAccount.balance}")
        }

        return BankAccountWithdrawalEvent(
            accountId = accountId,
            bankAccountId = fromBankAccountId,
            amount = amount
        )
    }

    fun processPendingTransaction(
        bankAccountId: UUID,
        transactionId: UUID,
    ): TransferTransactionProcessedEvent {
        val pendingTransaction = bankAccounts[bankAccountId]!!.pendingTransactions[transactionId]!!
        // todo sukhoa validation
        return TransferTransactionProcessedEvent(
            this.accountId,
            bankAccountId,
            transactionId
        )
    }

    fun rollbackPendingTransaction(
        bankAccountId: UUID,
        transactionId: UUID,
    ): TransferTransactionRollbackedEvent {
        val pendingTransaction = bankAccounts[bankAccountId]!!.pendingTransactions[transactionId]!!
        // todo sukhoa validation
        return TransferTransactionRollbackedEvent(
            this.accountId,
            bankAccountId,
            transactionId
        )
    }

    @StateTransitionFunc
    fun nothing(event: TransactionCancelEvent){

    }

    @StateTransitionFunc
    fun createNewBankAccount(event: AccountCreatedEvent) {
        accountId = event.accountId
        holderId = event.userId
    }

    @StateTransitionFunc
    fun createNewBankAccount(event: BankAccountCreatedEvent) {
        bankAccounts[event.bankAccountId] = BankAccount(event.bankAccountId)
    }

    @StateTransitionFunc
    fun deposit(event: BankAccountDepositEvent) {
        bankAccounts[event.bankAccountId]!!.deposit(event.amount)
    }

    @StateTransitionFunc
    fun withdraw(event: BankAccountWithdrawalEvent) {
        bankAccounts[event.bankAccountId]!!.withdraw(event.amount)
    }

    @StateTransitionFunc
    fun internalAccountTransfer(event: InternalAccountTransferEvent) {
        bankAccounts[event.bankAccountIdFrom]!!.withdraw(event.amount)
        bankAccounts[event.bankAccountIdTo]!!.deposit(event.amount)
    }

    @StateTransitionFunc
    fun acceptTransfer(event: TransferTransactionAcceptedEvent) {
        bankAccounts[event.bankAccountId]!!.initiatePendingTransaction(
            PendingTransaction(
                event.transactionId,
                event.transferAmount,
                event.isDeposit
            )
        )
    }

    @StateTransitionFunc
    fun processTransaction(event: TransferTransactionProcessedEvent) =
        bankAccounts[event.bankAccountId]!!.processPendingTransaction(event.transactionId)

    @StateTransitionFunc
    fun rollbackTransaction(event: TransferTransactionRollbackedEvent) =
        bankAccounts[event.bankAccountId]!!.rollbackPendingTransaction(event.transactionId)

    @StateTransitionFunc
    fun sendMoney(event:InternalAccountSendEvent){
        val bankAccount = bankAccounts[event.bankAccountIdFrom]
        bankAccount?.withdraw(event.amount)
    }

    @StateTransitionFunc
    fun receiveMoney(event:InternalAccountReceiveEvent){
        val bankAccount = bankAccounts[event.bankAccountIdTo]
        bankAccount?.deposit(event.amount)
    }

    @StateTransitionFunc
    fun externalAccountTransferDecline(event: TransferTransactionDeclinedEvent) {
        val bankAccount = bankAccounts[event.bankAccountId]
            ?: throw IllegalArgumentException("No such account to withdraw from: ${event.bankAccountId}")

        bankAccount.deposit(event.amount)
    }
}


data class BankAccount(
    val id: UUID,
    internal var balance: BigDecimal = BigDecimal.ZERO,
    internal var pendingTransactions: MutableMap<UUID, PendingTransaction> = mutableMapOf()
) {
    fun deposit(amount: BigDecimal) {
        this.balance = this.balance.add(amount)
    }

    fun withdraw(amount: BigDecimal) {
        this.balance = this.balance.subtract(amount)
    }

    fun initiatePendingTransaction(pendingTransaction: PendingTransaction) {
        if (!pendingTransaction.isDeposit) {
            withdraw(pendingTransaction.transferAmountFrozen)
        }
        pendingTransactions[pendingTransaction.transactionId] = pendingTransaction
    }

    fun processPendingTransaction(trId: UUID) {
        val pendingTransaction = pendingTransactions.remove(trId)!!
        if (pendingTransaction.isDeposit) {
            deposit(pendingTransaction.transferAmountFrozen)
        }
    }

    fun rollbackPendingTransaction(trId: UUID) {
        val pendingTransaction = pendingTransactions.remove(trId)!!
        if (!pendingTransaction.isDeposit) {
            deposit(pendingTransaction.transferAmountFrozen) // refund
        }
    }
}

data class PendingTransaction(
    val transactionId: UUID,
    val transferAmountFrozen: BigDecimal,
    val isDeposit: Boolean
)