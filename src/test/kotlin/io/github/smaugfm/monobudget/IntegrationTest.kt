package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.verify.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.KoinApplication
import org.koin.dsl.module
import java.net.URI

class IntegrationTest : TestBase(), CoroutineScope {
    private val apiMock = mockkClass(LunchmoneyApi::class)
    private val tgMock = mockkClass(TelegramApi::class)

    override fun KoinApplication.testKoinApplication() {
        setupKoinModules(
            this@IntegrationTest,
            Settings.load(
                IntegrationTest::class.java
                    .classLoader
                    .getResource("test-settings.yml")!!.path
            ),
            MonoWebhookSettings(false, URI.create("null://"), 0)
        )
        modules(
            module {
                single { apiMock }
                single { tgMock }
                single {
                    mockkClass(BudgetSettingsVerifier::class).also {
                        coEvery { it.verify() } returns Unit
                    }
                }
            }
        )
    }

    override val coroutineContext = Dispatchers.Default
}
