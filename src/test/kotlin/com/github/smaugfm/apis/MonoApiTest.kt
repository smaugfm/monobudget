package com.github.smaugfm.apis

import com.github.smaugfm.models.settings.Settings
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.days

@Suppress("DeferredResultUnused")
internal class MonoApiTest {
    private val settings = Settings.load(Paths.get("settings.json").readText())
    private val api = MonoApi(settings.monoTokens.first())

    @Test
    fun testAllEndpointsDontFail() {
        runBlocking {
            val info = assertDoesNotThrow { api.fetchUserInfo() }
            assertDoesNotThrow { api.fetchBankCurrency() }
            assertDoesNotThrow {
                api.fetchStatementItems(
                    info.accounts.first().id,
                    Clock.System.now().minus(30.days)
                )
            }
        }
    }
}
