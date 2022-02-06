package com.github.smaugfm.apis

import com.github.smaugfm.Util.checkAsync
import com.github.smaugfm.models.settings.Settings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@Suppress("DeferredResultUnused")
class YnabApiTest {
    init {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    }

    private val api = YnabApi(Settings.loadDefault())

    @Test
    fun testAllEndpointsDontFail() {
        runBlocking {
            val accountsDeferred = checkAsync { api.getAccounts() }
            checkAsync { api.getCategories() }
            checkAsync { api.getPayees() }
            accountsDeferred.await().let { accounts ->
                if (accounts.isNotEmpty()) {
                    assertDoesNotThrow { api.getAccount(accounts.first().id) }
                    assertDoesNotThrow { api.getAccountTransactions(accounts.first().id) }
                }
            }
        }
    }
}
