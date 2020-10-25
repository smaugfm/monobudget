package com.github.smaugfm.ynab

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.events.IEventsHandlerRegistrar
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.util.PayeeSuggestor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.hours

class YnabHandler(
    private val ynab: YnabApi,
    private val mappings: Mappings,
) : IEventsHandlerRegistrar {
    val payeeSuggestor = PayeeSuggestor()
    private lateinit var payees: List<YnabPayee>

    init {
        GlobalScope.launch {
            while (true) {
                payees = ynab.getPayees()
                delay(1.hours)
            }
        }
    }

    override fun registerEvents(builder: HandlersBuilder) {
        builder.apply {
            registerUnit(this@YnabHandler::createTransaction)
            registerUnit(this@YnabHandler::updateTransaction)
        }
    }

    suspend fun updateTransaction(e: Event.Ynab.TransactionAction) {
        val transactionDetail = ynab.getTransaction(e.type.transactionId)
        val saveTransaction = ynabTransactionSaveFromDetails(transactionDetail)

        val newTransaction = when (e.type) {
            is TransactionActionType.Uncategorize -> saveTransaction.copy(category_id = null)
            is TransactionActionType.Unpayee -> saveTransaction.copy(
                payee_id = null,
                payee_name = null,
                approved = false
            )
            is TransactionActionType.Unapprove -> saveTransaction.copy(approved = false)
            is TransactionActionType.Unknown -> saveTransaction.copy(
                payee_id = mappings.unknownPayeeId,
                category_id = mappings.unknownCategoryId,
                payee_name = null
            )
            is TransactionActionType.MakePayee -> saveTransaction.copy(payee_id = null, payee_name = e.type.payeeName)
        }

        ynab.updateTransaction(transactionDetail.id, newTransaction)
    }

    private suspend fun createTransaction(
        dispatcher: IEventDispatcher,
        e: Event.Mono.NewStatementReceived,
    ) {
        val saveTransaction = determineTransactionParams(e.data)
        val transactionDetail = ynab.createTransaction(saveTransaction)

        dispatcher(
            Event.Telegram.SendStatementMessage(
                e.data,
                transactionDetail,
            )
        )
    }

    private fun determineTransactionParams(data: MonoWebHookResponseData): YnabSaveTransaction {
        val suggestedPayee = payeeSuggestor(data.statementItem.description, payees.map { it.name }).firstOrNull()
        val mccCategoryOverride = mappings.getMccCategoryOverride(data.statementItem.mcc)

        return YnabSaveTransaction(
            account_id = mappings.getYnabAccByMono(data.account)
                ?: throw IllegalStateException("Could not find YNAB account for mono account ${data.account}"),
            date = data.statementItem.time.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            amount = data.statementItem.amount,
            payee_id = null,
            payee_name = suggestedPayee,
            category_id = mccCategoryOverride,
            memo = data.statementItem.description,
            cleared = YnabCleared.Cleared,
            approved = true,
            flag_color = null,
            import_id = null,
            subtransactions = emptyList()
        )
    }

    companion object {
        fun ynabTransactionSaveFromDetails(t: YnabTransactionDetail): YnabSaveTransaction {
            return YnabSaveTransaction(
                t.account_id,
                t.date,
                t.amount,
                t.payee_id,
                null,
                t.category_id,
                t.memo,
                t.cleared,
                t.approved,
                t.flag_color,
                t.import_id,
                t.subtransactions.map {
                    YnabSaveSubTransaction(
                        it.amount,
                        it.payee_id,
                        it.payee_name,
                        it.category_id,
                        it.memo
                    )
                }
            )
        }
    }
}
