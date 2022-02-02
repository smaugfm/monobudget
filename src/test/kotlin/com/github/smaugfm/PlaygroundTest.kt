package com.github.smaugfm

import com.github.smaugfm.models.YnabCleared
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.mono.MonoApi
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
    fun `Transfer transactions test`() {
        val ynab = YnabApi(
            "94ecb102846f44a1d2c35a296f8a0b315d50d3715355f85c9cc5676ebd8d2996",
            "692ab9be-240a-4847-96c1-80aa21709e9c"
        )

        runBlocking {
            val accounts = ynab.getAccounts()
            val transactions = ynab.getAccountTransactions(accounts[0].id)
            val targetAccount = accounts[1]
            transactions[1].toSaveTransaction().let {
                val result =
                    ynab.updateTransaction(transactions[1].id, it.copy(payee_id = targetAccount.transfer_payee_id))
                val targetTransferTransaction = ynab.getTransaction(result.transfer_transaction_id!!)
                assert(targetTransferTransaction.id == result.transfer_transaction_id)
                ynab.updateTransaction(
                    targetTransferTransaction.id,
                    targetTransferTransaction.toSaveTransaction().copy(cleared = YnabCleared.cleared)
                )
            }
        }
    }

    @Test
    @Disabled
    fun `Get last 29 days Mono transactions`() {
        runBlocking {
            val window = 30
            val untilDays = 30

            val settings = Settings.loadDefault()
            val mono = MonoApi(settings.monoTokens[0])
            val ynab = YnabApi(settings.ynabToken, settings.ynabBudgetId)
            val statementItems = mono.fetchStatementItems(
                settings.mappings.getMonoAccounts()
                    .find { it.startsWith("p") }!!,
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
