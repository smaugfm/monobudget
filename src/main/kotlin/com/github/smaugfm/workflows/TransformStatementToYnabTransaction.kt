package com.github.smaugfm.workflows

import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.models.MonoWebHookResponseData
import com.github.smaugfm.models.YnabCleared
import com.github.smaugfm.models.YnabPayee
import com.github.smaugfm.models.YnabSaveTransaction
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.util.PayeeSuggestor
import com.github.smaugfm.util.replaceNewLines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import kotlin.time.Duration.Companion.hours

private const val MONO_TO_YNAB_ADJUST = 10

private val logger = KotlinLogging.logger {}

class TransformStatementToYnabTransaction(
    scope: CoroutineScope,
    private val mappings: Mappings,
    private val ynabApi: YnabApi,
) {
    @Volatile
    private var payees: Deferred<List<YnabPayee>>
    private val payeeSuggestor = PayeeSuggestor()

    init {
        logger.debug { "Launching periodic getPayees fetching." }
        val initialDeferred = CompletableDeferred<List<YnabPayee>>()
        payees = initialDeferred
        scope.launch(context = Dispatchers.IO) {
            logger.debug { "Loop getPayees periodic" }
            while (true) {
                val result = ynabApi.getPayees()
                if (payees === initialDeferred)
                    initialDeferred.complete(result)
                else
                    payees = CompletableDeferred(result)
                delay(1.hours)
            }
        }
    }

    suspend operator fun invoke(response: MonoWebHookResponseData): YnabSaveTransaction {
        logger.debug { "Transforming Monobank statement to Ynab transaction." }
        val suggestedPayee =
            payeeSuggestor(response.statementItem.description, payees.await().map { it.name })
                .firstOrNull()
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
