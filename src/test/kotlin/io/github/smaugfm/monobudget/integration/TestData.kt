package io.github.smaugfm.monobudget.integration

import io.github.smaugfm.lunchmoney.model.LunchmoneyCategoryMultiple
import java.time.Instant
import java.util.Currency

object TestData {
    val UAH = Currency.getInstance("UAH")

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
