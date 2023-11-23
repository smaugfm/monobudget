package io.github.smaugfm.monobudget.integration

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyCategoryMultiple
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.Application
import io.github.smaugfm.monobudget.TestBase
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.common.verify.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.github.smaugfm.monobudget.setupKoinModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths
import kotlin.coroutines.cancellation.CancellationException

private val log = KotlinLogging.logger {}

class IntegrationTest : TestBase(), CoroutineScope {
    @MockK
    lateinit var lunchmoneyMock: LunchmoneyApi

    @MockK
    lateinit var tgMock: TelegramApi

    @MockK
    lateinit var periodicFetcherFactory: PeriodicFetcherFactory

    @MockK
    lateinit var categoriesFetcherMock:
        PeriodicFetcherFactory.PeriodicFetcher<List<LunchmoneyCategoryMultiple>>

    @MockK
    lateinit var budgetSettingsVerifier: BudgetSettingsVerifier

    override fun KoinApplication.testKoinApplication() {
        every { tgMock.start(any()) } answers {
            this@IntegrationTest.launch {}
        }
        every {
            periodicFetcherFactory.create<List<LunchmoneyCategoryMultiple>>(
                "Lunchmoney categories",
                any(),
            )
        } returns categoriesFetcherMock
        coEvery {
            categoriesFetcherMock.fetched()
        } returns TestData.categories
        coEvery { budgetSettingsVerifier.verify() } returns Unit

        setupKoinModules(
            this@IntegrationTest,
            Settings.load(
                Paths.get(
                    IntegrationTest::class.java.classLoader.getResource("test-settings.yml")!!.path,
                ),
            ),
            MonoWebhookSettings(false, URI.create(""), 0),
        )
        modules(
            module {
                single { lunchmoneyMock }
                single { tgMock }
                single { budgetSettingsVerifier } bind ApplicationStartupVerifier::class
                single { periodicFetcherFactory }
            },
        )
    }

    override val coroutineContext = Dispatchers.Default

    @Test
    fun `Runs and finishes in one second`() {
        runTestApplication {
            delay(1000)
        }
    }

    private fun runTestApplication(block: suspend () -> Unit) {
        assertThrows<CancellationException> {
            runBlocking(coroutineContext) {
                Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>().also {
                    launch {
                        it.run()
                    }
                    block()
                    log.info { "Shutting down" }
                    coroutineContext.cancel()
                }
            }
        }
    }
}
