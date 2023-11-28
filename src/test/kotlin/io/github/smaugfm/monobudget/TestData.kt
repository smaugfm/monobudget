package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.model.LunchmoneyCategoryMultiple
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import kotlinx.datetime.Clock
import java.time.Instant
import java.util.Currency
import java.util.UUID

object TestData {
    val UAH: Currency = Currency.getInstance("UAH")

    fun exampleStatement1(description: String) =
        MonobankWebhookResponseStatementItem(
            d =
                MonoWebhookResponseData(
                    account = "MONO-EXAMPLE-UAH",
                    statementItem =
                        MonoStatementItem(
                            id = UUID.randomUUID().toString(),
                            time = Clock.System.now(),
                            description = description,
                            mcc = 4829,
                            originalMcc = 4829,
                            hold = true,
                            amount = 5600,
                            operationAmount = 5600,
                            currencyCode = UAH,
                            commissionRate = 0,
                            cashbackAmount = 0,
                            balance = 0,
                        ),
                ),
            accountCurrency = UAH,
        )

    fun exampleStatement2(description: String) =
        MonobankWebhookResponseStatementItem(
            d =
                MonoWebhookResponseData(
                    account = "MONO-EXAMPLE-UAH2",
                    statementItem =
                        MonoStatementItem(
                            id = UUID.randomUUID().toString(),
                            time = Clock.System.now(),
                            description = description,
                            mcc = 4829,
                            originalMcc = 4829,
                            hold = true,
                            amount = -5600,
                            operationAmount = -5600,
                            currencyCode = UAH,
                            commissionRate = 0,
                            cashbackAmount = 0,
                            balance = 0,
                        ),
                ),
            accountCurrency = UAH,
        )

    val categories =
        listOf(
            LunchmoneyCategoryMultiple(
                id = 444443,
                name = "Авто",
                description = null,
                isIncome = false,
                excludeFromBudget = false,
                excludeFromTotals = false,
                updatedAt = Instant.parse("2023-03-26T08:11:06.088Z"),
                createdAt = Instant.parse("2023-03-26T08:11:06.088Z"),
                isGroup = false,
                groupId = null,
            ),
            LunchmoneyCategoryMultiple(
                id = 444444,
                name = "Перекази",
                description = null,
                isIncome = false,
                excludeFromBudget = true,
                excludeFromTotals = true,
                updatedAt = Instant.parse("2023-03-26T08:47:50.132Z"),
                createdAt = Instant.parse("2023-03-26T08:47:50.132Z"),
                isGroup = false,
                groupId = null,
            ),
            LunchmoneyCategoryMultiple(
                id = 444445,
                name = "Розваги",
                description = null,
                isIncome = false,
                excludeFromBudget = false,
                excludeFromTotals = false,
                updatedAt = Instant.parse("2023-03-26T08:11:24.380Z"),
                createdAt = Instant.parse("2023-03-26T08:11:24.380Z"),
                isGroup = false,
                groupId = null,
            ),
            LunchmoneyCategoryMultiple(
                id = 444446,
                name = "Транспорт",
                description = null,
                isIncome = false,
                excludeFromBudget = false,
                excludeFromTotals = false,
                updatedAt = Instant.parse("2023-03-26T08:11:40.931Z"),
                createdAt = Instant.parse("2023-03-26T08:11:40.931Z"),
                isGroup = false,
                groupId = null,
            ),
        )
}
