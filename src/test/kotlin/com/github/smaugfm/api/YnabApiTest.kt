package com.github.smaugfm.api

import com.github.smaugfm.models.settings.Settings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Paths
import kotlin.io.path.readText

@Suppress("DeferredResultUnused")
class YnabApiTest {
    init {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    }

    private val api = YnabApi(Settings.load(Paths.get("settings.json").readText()))

    @Test
    @Disabled
    fun testAllEndpointsDontFail() {
        runBlocking {
            val accountsDeferred = assertDoesNotThrow { api.getAccounts() }
            assertDoesNotThrow { api.getCategories() }
            assertDoesNotThrow { api.getPayees() }
            accountsDeferred.let { accounts ->
                if (accounts.isNotEmpty()) {
                    assertDoesNotThrow { api.getAccount(accounts.first().id) }
                    assertDoesNotThrow { api.getAccountTransactions(accounts.first().id) }
                }
            }
        }
    }
}
