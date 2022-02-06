package com.github.smaugfm.workflows

import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.models.MonoStatementItem
import com.github.smaugfm.models.MonoWebHookResponseData
import com.github.smaugfm.models.settings.Settings
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Currency
import java.util.concurrent.CancellationException

internal class TransformStatementToYnabTransactionTest {
    init {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
    }
    val settings = Settings.loadDefault()
    val testStatement = MonoWebHookResponseData(
        account = settings.mappings.getMonoAccounts().first(),
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
            hold = true
        )
    )

    @Test
    fun testTransform() {
        assertThrows<CancellationException> {
            runBlocking {
                val transform =
                    TransformStatementToYnabTransaction(
                        this@runBlocking,
                        settings.mappings,
                        YnabApi(settings)
                    )
                val transaction = transform.invoke(testStatement)
                println("Result: $transaction")
                this.cancel("Finished.")
            }
        }
    }
}
