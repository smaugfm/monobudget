package io.github.smaugfm.monobudget.service

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.settings.Settings
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.transaction.CategorySuggestingService
import io.github.smaugfm.monobudget.service.transaction.PayeeSuggestingService
import io.github.smaugfm.monobudget.service.ynab.MonoStatementToYnabTransactionTransformer
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

internal class TransformStatementToYnabTransactionTest {
    init {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    }

    val settings = Settings.load(Paths.get("settings.json").readText())
    val monoAccountsService = MonoAccountsService(settings)
    val testStatement = MonoWebhookResponseData(
        account = monoAccountsService.getMonoAccounts().first(),
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
                    MonoStatementToYnabTransactionTransformer(
                        this@runBlocking,
                        MonoAccountsService(settings),
                        PayeeSuggestingService(),
                        CategorySuggestingService(settings),
                        YnabApi(settings)
                    )
                val transaction = transform.invoke(testStatement)
                println("Result: $transaction")
                this.cancel("Finished.")
            }
        }
    }
}
