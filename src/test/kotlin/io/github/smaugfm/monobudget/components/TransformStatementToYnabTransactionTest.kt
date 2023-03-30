package io.github.smaugfm.monobudget.components

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.misc.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.ynab.YnabApi
import io.github.smaugfm.monobudget.ynab.YnabCategorySuggestionService
import io.github.smaugfm.monobudget.ynab.YnabNewTransactionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.nio.file.Paths
import java.util.Currency
import java.util.concurrent.CancellationException
import kotlin.io.path.readText

@Disabled
@OptIn(DelicateCoroutinesApi::class)
internal class TransformStatementToYnabTransactionTest : KoinTest {
    val transform: YnabNewTransactionFactory by inject()

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val settings = io.github.smaugfm.monobudget.common.model.Settings.load(Paths.get("settings.yml").readText())
            startKoin {
                modules(
                    module {
                        single<CoroutineScope> { GlobalScope }
                        single { PeriodicFetcherFactory() }
                        single { settings.mono }
                        single { settings.budgetBackend as YNAB } bind BudgetBackend::class
                        single { MonoAccountsService() }
                        single { YnabApi() }
                        single { StringSimilarityPayeeSuggestionService() }
                        single { YnabCategorySuggestionService() }
                    }
                )
            }
        }
    }

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
                val transaction = transform.create(testStatement)
                println("Result: $transaction")
                this.cancel("Finished.")
            }
        }
    }
}
