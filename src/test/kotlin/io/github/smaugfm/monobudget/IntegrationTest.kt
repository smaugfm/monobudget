package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.verify.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.mono.MonoApi
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.koin.core.KoinApplication
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

class IntegrationTest : TestBase(), CoroutineScope {
    private val lunchmoneyMock = mockkClass(LunchmoneyApi::class)
    private val tgMock = mockkClass(TelegramApi::class)

    override fun KoinApplication.testKoinApplication() {
        setupKoinModules(
            this@IntegrationTest,
            Settings.load(
                Paths.get(
                    IntegrationTest::class.java
                        .classLoader
                        .getResource("test-settings.yml")!!.path
                )
            ),
            MonoWebhookSettings(false, URI.create(""), 0)
        )
        modules(
            module {
                single { lunchmoneyMock }
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

    @Test
    fun contextLoads() {
        val budgetBackend = getKoin().get<BudgetBackend>()

        println(budgetBackend)
        // runBlocking(coroutineContext) {
        //     Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>().run()
        // }
    }
}
