package io.github.smaugfm.monobudget.api

import io.github.smaugfm.monobudget.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.model.Settings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Paths
import kotlin.io.path.readText

@Disabled
class YnabApiTest {
    private val settings = Settings.load(Paths.get("settings.json").readText())
    private val api = YnabApi(settings.budgetBackend as YNAB)

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
