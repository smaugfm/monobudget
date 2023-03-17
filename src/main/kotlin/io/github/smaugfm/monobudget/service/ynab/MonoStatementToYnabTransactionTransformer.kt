package io.github.smaugfm.monobudget.service.ynab

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.ynab.YnabCleared
import io.github.smaugfm.monobudget.models.ynab.YnabPayee
import io.github.smaugfm.monobudget.models.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.transaction.CategorySuggestingService
import io.github.smaugfm.monobudget.service.transaction.PayeeSuggestingService
import io.github.smaugfm.monobudget.util.replaceNewLines
import io.ktor.util.logging.error
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

class MonoStatementToYnabTransactionTransformer(
    scope: CoroutineScope,
    private val monoAccountsService: MonoAccountsService,
    private val payeeSuggestingService: PayeeSuggestingService,
    private val categorySuggestingService: CategorySuggestingService,
    private val ynabApi: YnabApi
) {
    @Volatile
    private var payees: Deferred<List<YnabPayee>>

    init {
        logger.debug { "Launching periodic getPayees fetching." }
        val initialDeferred = CompletableDeferred<List<YnabPayee>>()
        payees = initialDeferred
        scope.launch(context = Dispatchers.IO) {
            logger.debug { "Loop getPayees periodic" }
            while (true) {
                val result = try {
                    ynabApi.getPayees()
                } catch (e: Throwable) {
                    logger.error(e)
                    continue
                }
                if (payees === initialDeferred) {
                    initialDeferred.complete(result)
                } else {
                    payees = CompletableDeferred(result)
                }
                delay(1.hours)
            }
        }
    }

    suspend operator fun invoke(response: MonoWebhookResponseData): YnabSaveTransaction {
        logger.debug { "Transforming Monobank statement to Ynab transaction." }
        val suggestedPayee =
            payeeSuggestingService.suggest(
                response.statementItem.description ?: "",
                payees.await().map { it.name }
            )
                .firstOrNull()
        val mccCategoryOverride = categorySuggestingService.suggestByMcc(response.statementItem.mcc)

        return YnabSaveTransaction(
            accountId = monoAccountsService.getYnabAccByMono(response.account)
                ?: error("Could not find YNAB account for mono account ${response.account}"),
            date = response.statementItem.time.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            amount = response.statementItem.amount * MONO_TO_YNAB_ADJUST,
            payeeId = null,
            payeeName = suggestedPayee,
            categoryId = mccCategoryOverride,
            memo = (response.statementItem.description ?: "").replaceNewLines(),
            cleared = YnabCleared.Cleared,
            approved = true,
            flagColor = null,
            importId = null,
            subtransactions = emptyList()
        )
    }
}
