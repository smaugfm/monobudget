package io.github.smaugfm.monobudget.components

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.components.suggestion.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.components.suggestion.YnabCategorySuggestionService
import io.github.smaugfm.monobudget.components.transaction.factory.YnabNewTransactionFactory
import io.github.smaugfm.monobudget.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import java.util.Currency
import java.util.concurrent.CancellationException
import kotlin.io.path.readText

@Disabled
@OptIn(DelicateCoroutinesApi::class)
internal class TransformStatementToYnabTransactionTest {
    private val periodicFetcherFactory = PeriodicFetcherFactory(GlobalScope)
    private val settings = Settings.load(Paths.get("settings.yml").readText())
    private val api = YnabApi(settings.budgetBackend as YNAB)
    private val testStatement = MonoWebhookResponseData(
        account = "vasa",
        statementItem = MonoStatementItem(
            id = "F8NpbBKuBu2CgubD",
            time = Instant.parse("2022-02-06T11:42:40Z"),
            description = "Від: Дмитро Марчук",
            mcc = 4829,
            amount = 100,
            operationAmount = 100,
            currencyCode = Currency.getInstance("UAH"),
            comment = "",
            commissionRate = 0,
            cashbackAmount = 0,
            balance = 1674202,
            hold = true,
            originalMcc = 4829,
            receiptId = "vasa",
            invoiceId = "vasa",
            counterEdrpou = "vasa",
            counterIban = "vasa",
            counterName = "vasa"
        )
    )

    @Test
    @Disabled
    fun testTransform() {
        assertThrows<CancellationException> {
            runBlocking {
                val transform =
                    YnabNewTransactionFactory(
                        periodicFetcherFactory,
                        MonoAccountsService(periodicFetcherFactory, settings.mono),
                        StringSimilarityPayeeSuggestionService(),
                        YnabCategorySuggestionService(periodicFetcherFactory, settings.mcc, api),
                        YnabApi(settings.budgetBackend as YNAB)
                    )
                val transaction = transform.create(testStatement)
                println("Result: $transaction")
                this.cancel("Finished.")
            }
        }
    }
}
