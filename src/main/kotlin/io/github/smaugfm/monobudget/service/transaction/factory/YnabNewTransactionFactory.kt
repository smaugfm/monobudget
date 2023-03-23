package io.github.smaugfm.monobudget.service.transaction.factory

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.ynab.YnabCleared
import io.github.smaugfm.monobudget.models.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.suggesting.CategorySuggestingService
import io.github.smaugfm.monobudget.service.suggesting.StringSimilarityPayeeSuggestingService
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.util.toLocalDateTime
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class YnabNewTransactionFactory(
    periodicFetcherFactory: PeriodicFetcherFactory,
    monoAccountsService: MonoAccountsService,
    private val payeeSuggestingService: StringSimilarityPayeeSuggestingService,
    categorySuggestingService: CategorySuggestingService,
    private val ynabApi: YnabApi
) : NewTransactionFactory<YnabSaveTransaction>(monoAccountsService, categorySuggestingService) {
    private val payeesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) { ynabApi.getPayees() }

    override suspend fun create(response: MonoWebhookResponseData): YnabSaveTransaction {
        log.debug { "Transforming Monobank statement to Ynab transaction." }

        val suggestedPayee =
            payeeSuggestingService.suggest(
                response.statementItem.description ?: "",
                payeesFetcher.getData().map { it.name }
            ).firstOrNull()

        return with(response.statementItem) {
            YnabSaveTransaction(
                accountId = getBudgetAccountId(response),
                date = time.toLocalDateTime().date,
                amount = convertMonobankAmountToYnabAmount(amount),
                payeeId = null,
                payeeName = suggestedPayee,
                categoryId = getCategoryId(response),
                memo = formatDescription(),
                cleared = YnabCleared.Cleared,
                approved = true,
                flagColor = null,
                importId = null,
                subtransactions = emptyList()
            )
        }
    }

    private fun convertMonobankAmountToYnabAmount(amount: Long): Long {
        // Monobank amount is in minimum currency units (e.g. cents for dollars)
        // and YNAB amount is in milliunits (1/1000th of a dollar)

        return amount * MONO_TO_YNAB_ADJUST
    }
    companion object {
        private const val MONO_TO_YNAB_ADJUST = 10
    }
}

