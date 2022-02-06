package com.github.smaugfm.apis

import com.github.smaugfm.Util.checkAsync
import com.github.smaugfm.models.settings.Settings
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days

@Suppress("DeferredResultUnused")
internal class MonoApiTest {
    private val settings = Settings.loadDefault()
    private val api = MonoApi(settings.monoTokens.first())

    @Test
    fun testAllEndpointsDontFail() {
        runBlocking {
            val info = checkAsync { api.fetchUserInfo() }.await()
            checkAsync { api.fetchBankCurrency() }
            checkAsync {
                api.fetchStatementItems(
                    info.accounts.first().id,
                    Clock.System.now().minus(30.days)
                )
            }
        }
    }
}
