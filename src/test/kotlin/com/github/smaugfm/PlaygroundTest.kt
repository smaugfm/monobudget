package com.github.smaugfm

import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.PayeeSuggestor
import com.github.smaugfm.ynab.YnabApi
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class PlaygroundTest {
    @Test
    @Disabled
    fun `Get last 29 days Mono transactions`() {
        runBlocking {
            val window = 30
            val untilDays = 30

            val settings = Settings.loadDefault()
            val mono = MonoApi(settings.monoTokens.drop(1).first())
            val ynab = YnabApi(settings.ynabToken, settings.ynabBudgetId)
            val statementItems = mono.fetchStatementItems(
                settings.mappings.getMonoAccounts()
                    .find { it.startsWith("3") }!!,
                Clock.System.now() - Duration.days((window + untilDays)),
                Clock.System.now() - Duration.days(untilDays),
            )

            val payees = ynab.getPayees()
            val payeeNames = payees.map { it.name }
            val suggestor = PayeeSuggestor()

            val mccs = listOf(4829)
            statementItems
                .filter { it.mcc !in mccs }
                .filter { settings.mappings.getMccCategoryOverride(it.mcc) == null }
                .filter {
                    suggestor(it.description, payeeNames).isEmpty()
                }
                .forEach {
                    val suggestions = suggestor(it.description, payeeNames)

                    println(
                        "Desc: ${it.description}, mcc: ${it.mcc}-${MCC.mapRussian[it.mcc]}\n\tSuggested payees: ${
                        suggestions.joinToString(", ")
                        }\n"
                    )
                }
        }
    }
}
