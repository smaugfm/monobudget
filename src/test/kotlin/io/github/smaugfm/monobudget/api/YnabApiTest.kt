package io.github.smaugfm.monobudget.api

import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.ynab.YnabApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.nio.file.Paths
import kotlin.io.path.readText

@Disabled
class YnabApiTest : KoinTest {
    private val api: YnabApi by inject()

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

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val settings = io.github.smaugfm.monobudget.common.model.Settings.load(Paths.get("settings.yml").readText())
            startKoin {
                modules(
                    module {
                        single { settings.budgetBackend as YNAB } bind BudgetBackend::class
                        single { YnabApi() }
                    }
                )
            }
        }
    }
}
