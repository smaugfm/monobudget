package io.github.smaugfm.monobudget.integration

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyCategoryMultiple
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.Application
import io.github.smaugfm.monobudget.TestBase
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.common.verify.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.integration.TestData.UAH
import io.github.smaugfm.monobudget.mono.MonoWebhookListener
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import io.github.smaugfm.monobudget.setupKoinModules
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

private val log = KotlinLogging.logger {}

class IntegrationTest : TestBase(), CoroutineScope {

    override val coroutineContext = Dispatchers.Default

    @MockK
    lateinit var lunchmoneyMock: LunchmoneyApi

    @MockK
    lateinit var tgMock: TelegramApi

    @MockK
    lateinit var periodicFetcherFactory: PeriodicFetcherFactory

    @MockK
    lateinit var webhookListener: MonoWebhookListener
    private val webhookStatementsFlow = MutableSharedFlow<StatementProcessingContext>()

    @MockK
    lateinit var categoriesFetcherMock:
        PeriodicFetcherFactory.PeriodicFetcher<List<LunchmoneyCategoryMultiple>>

    @MockK
    lateinit var budgetSettingsVerifier: BudgetSettingsVerifier

    @Test
    fun `When nothing happens finishes normally`() {
        runTestApplication {
            delay(100)
        }
    }

    @Test
    fun `Mono webhook triggers new transaction creation`() {
        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d = MonoWebhookResponseData(
                            account = "MONO-EXAMPLE-UAH",
                            statementItem = MonoStatementItem(
                                id = UUID.randomUUID().toStr(),
                                time = Clock.System.now(),
                                description = "test",
                                mcc = 4829,
                                originalMcc = 4829,
                                hold = true,
                                amount = -5600,
                                operationAmount = -5600,
                                currencyCode = UAH,
                                commissionRate = 0,
                                cashbackAmount = 0,
                                balance = 0,
                            )
                        ), accountCurrency = UAH
                    )
                )
            )

        }
    }

    override fun KoinApplication.testKoinApplication() {
        coEvery { webhookListener.prepare() } just runs
        coEvery { webhookListener.statements() } returns webhookStatementsFlow

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
        coEvery { budgetSettingsVerifier.verify() } just runs

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
                single { webhookListener }
                single { tgMock }
                single { budgetSettingsVerifier } bind ApplicationStartupVerifier::class
                single { periodicFetcherFactory }
            },
        )
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
