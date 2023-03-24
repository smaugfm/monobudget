package io.github.smaugfm.monobudget.api

import io.github.smaugfm.monobank.MonobankPersonalApi
import io.github.smaugfm.monobudget.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.formatAmount
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalSerializationApi::class)
class YnabApiTest {
    private val settings = Settings.load(Paths.get("settings.json").readText())
    private val api = YnabApi(
        settings.budgetBackend as YNAB
    )

    @Disabled
    @Test
    fun mono() {
        val accountId = ""
        val api = MonobankPersonalApi("")
        val statements = api.getClientStatements(accountId, Clock.System.now().minus(31.days)).block()!!
        val str = statements.joinToString("\n") { item ->
            "${item.description} \t\t\t\t ${item.currencyCode.formatAmount(item.amount)} ${item.currencyCode}\t" +
                " ${item.mcc} ${
                MCC.map[item.mcc]?.let {
                    "${it.fullDescription} (${it.group.type} ${it.group.description}) "
                }
                }" +
                "${item.time.toLocalDateTime(TimeZone.currentSystemDefault())}"
        }
        println(str)
    }

    @Suppress("UNUSED_VARIABLE")
    @Test
    @Disabled
    fun testAllEndpointsDontFail() {
        runBlocking {
            val accountsDeferred = assertDoesNotThrow { api.getAccounts() }
            val categoryGroups = assertDoesNotThrow { api.getCategoryGroups() }
            accountsDeferred.let { accounts ->
                if (accounts.isNotEmpty()) {
                    assertDoesNotThrow { api.getAccount(accounts.first().id) }
                    assertDoesNotThrow { api.getAccountTransactions(accounts.first().id) }
                }
            }
        }
    }
}
