package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.misc.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.github.smaugfm.monobudget.common.util.toLocalDateTime
import io.github.smaugfm.monobudget.ynab.model.YnabCleared
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single(createdAtStart = true)
class YnabNewTransactionFactory(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val payeeSuggestingService: StringSimilarityPayeeSuggestionService,
    private val ynabApi: YnabApi
) : NewTransactionFactory<YnabSaveTransaction>() {

    private val payeesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) { ynabApi.getPayees() }

    override suspend fun create(statement: StatementItem): YnabSaveTransaction {
        log.debug { "Transforming Monobank statement to Ynab transaction." }

        val suggestedPayee =
            payeeSuggestingService.suggest(
                statement.description ?: "",
                payeesFetcher.getData().map { it.name }
            ).firstOrNull()

        return with(statement) {
            YnabSaveTransaction(
                accountId = getBudgetAccountId(statement),
                date = time.toLocalDateTime().date,
                amount = getAmount(),
                payeeId = null,
                payeeName = suggestedPayee,
                categoryId = getCategoryId(statement),
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
    private fun StatementItem.getAmount(): Long {
        return amount * MONO_TO_YNAB_ADJUST
    }

    companion object {
        private const val MONO_TO_YNAB_ADJUST = 10
    }
}
