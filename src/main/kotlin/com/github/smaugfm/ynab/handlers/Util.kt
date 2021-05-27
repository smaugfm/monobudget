package com.github.smaugfm.ynab.handlers

import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.util.PayeeSuggestor
import com.github.smaugfm.util.replaceNewLines
import com.github.smaugfm.ynab.YnabCleared
import com.github.smaugfm.ynab.YnabPayee
import com.github.smaugfm.ynab.YnabSaveTransaction
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

private val payeeSuggestor = PayeeSuggestor()
private const val MONO_TO_YNAB_ADJUST = 10

@OptIn(DelicateCoroutinesApi::class)
class MonoWebhookResponseToYnabTransactionConverter(
    private val mappings: Mappings,
    private val getPayees: suspend () -> List<YnabPayee>,
) {
    private lateinit var payees: List<YnabPayee>

    init {
        GlobalScope.launch {
            while (true) {
                payees = getPayees()
                delay(Duration.hours(1))
            }
        }
    }

    operator fun invoke(
        response: MonoWebHookResponseData,
    ): YnabSaveTransaction {
        val suggestedPayee = payeeSuggestor(response.statementItem.description, payees.map { it.name }).firstOrNull()
        val mccCategoryOverride = mappings.getMccCategoryOverride(response.statementItem.mcc)

        return YnabSaveTransaction(
            account_id = mappings.getYnabAccByMono(response.account)
                ?: throw IllegalStateException("Could not find YNAB account for mono account ${response.account}"),
            date = response.statementItem.time.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            amount = response.statementItem.amount * MONO_TO_YNAB_ADJUST,
            payee_id = null,
            payee_name = suggestedPayee,
            category_id = mccCategoryOverride,
            memo = response.statementItem.description.replaceNewLines(),
            cleared = YnabCleared.cleared,
            approved = true,
            flag_color = null,
            import_id = null,
            subtransactions = emptyList()
        )
    }
}
