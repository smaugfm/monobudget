package io.github.smaugfm.monobudget.service.statement

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.ynab.YnabCleared
import io.github.smaugfm.monobudget.models.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.suggesting.CategorySuggestingService
import io.github.smaugfm.monobudget.service.suggesting.StringSimilarityPayeeSuggestingService
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.util.replaceNewLines
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging

private const val MONO_TO_YNAB_ADJUST = 10

private val log = KotlinLogging.logger {}

class YnabNewTransactionFactory(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val monoAccountsService: MonoAccountsService,
    private val payeeSuggestingService: StringSimilarityPayeeSuggestingService,
    private val categorySuggestingService: CategorySuggestingService,
    private val ynabApi: YnabApi
) : NewTransactionFactory<YnabSaveTransaction>() {
    private val payeesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) { ynabApi.getPayees() }

    override suspend fun create(response: MonoWebhookResponseData): YnabSaveTransaction {
        log.debug { "Transforming Monobank statement to Ynab transaction." }
        val suggestedPayee =
            payeeSuggestingService.suggest(
                response.statementItem.description ?: "",
                payeesFetcher.data.await().map { it.name }
            )
                .firstOrNull()
        val categoryId = categorySuggestingService.mapNameToCategoryId(response.statementItem.mcc)

        return YnabSaveTransaction(
            accountId = monoAccountsService.getBudgetAccountId(response.account)
                ?: error("Could not find YNAB account for mono account ${response.account}"),
            date = response.statementItem.time.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            amount = response.statementItem.amount * MONO_TO_YNAB_ADJUST,
            payeeId = null,
            payeeName = suggestedPayee,
            categoryId = categoryId,
            memo = (response.statementItem.description ?: "").replaceNewLines(),
            cleared = YnabCleared.Cleared,
            approved = true,
            flagColor = null,
            importId = null,
            subtransactions = emptyList()
        )
    }
}
