package io.github.smaugfm.monobudget.components.transaction.factory

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.components.suggestion.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.model.ynab.YnabCleared
import io.github.smaugfm.monobudget.model.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.util.toLocalDateTime
import mu.KotlinLogging
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

class YnabNewTransactionFactory : NewTransactionFactory<YnabSaveTransaction>() {
    private val periodicFetcherFactory: PeriodicFetcherFactory by inject()
    private val payeeSuggestingService: StringSimilarityPayeeSuggestionService by inject()
    private val ynabApi: YnabApi by inject()

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
                amount = ynabAmount(),
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

    /**
     * Monobank amount uses minimum currency units (e.g. cents for dollars)
     * and YNAB amount uses milliunits (1/1000th of a dollar)
     */
    private fun MonoStatementItem.ynabAmount(): Long {
        return amount * MONO_TO_YNAB_ADJUST
    }

    companion object {
        private const val MONO_TO_YNAB_ADJUST = 10
    }
}
