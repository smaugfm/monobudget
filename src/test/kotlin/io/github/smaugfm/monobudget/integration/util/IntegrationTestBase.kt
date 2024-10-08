package io.github.smaugfm.monobudget.integration.util

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyCategory
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.response.LunchmoneyUpdateTransactionResponse
import io.github.smaugfm.monobudget.Application
import io.github.smaugfm.monobudget.TestBase
import io.github.smaugfm.monobudget.TestData
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.notify.TelegramApi
import io.github.smaugfm.monobudget.common.retry.InMemoryStatementRetryRepository
import io.github.smaugfm.monobudget.common.retry.StatementRetryRepository
import io.github.smaugfm.monobudget.common.startup.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.common.startup.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.common.statement.StatementSource
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.util.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.integration.TransactionsTest
import io.github.smaugfm.monobudget.mono.MonoWebhookListener
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.github.smaugfm.monobudget.setupKoinModules
import io.mockk.coEvery
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.junit.jupiter.api.assertThrows
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import reactor.core.publisher.Mono
import java.net.URI
import java.nio.file.Paths
import kotlin.coroutines.cancellation.CancellationException

private val log = KotlinLogging.logger {}

@Suppress("MagicNumber", "LongMethod")
abstract class IntegrationTestBase : TestBase(), CoroutineScope {
    override val coroutineContext = Dispatchers.Default

    @MockK
    lateinit var lunchmoneyMock: LunchmoneyApi

    @MockK
    lateinit var tgMock: TelegramApi

    @MockK
    lateinit var periodicFetcherFactory: PeriodicFetcherFactory

    @MockK
    lateinit var webhookListener: MonoWebhookListener
    protected val webhookStatementsFlow = MutableSharedFlow<StatementProcessingContext>(100)

    @MockK
    lateinit var categoriesFetcherMock:
        PeriodicFetcherFactory.PeriodicFetcher<List<LunchmoneyCategory>>

    @MockK
    lateinit var budgetSettingsVerifier: BudgetSettingsVerifier

    override fun testKoinApplication(app: KoinApplication) {
        coEvery { webhookListener.prepare() } just runs
        coEvery { webhookListener.statements() } returns webhookStatementsFlow

        coEvery { tgMock.sendMessage(any(), any(), any(), any(), any()) } returns mockk()
        every { tgMock.start(any()) } answers {
            this@IntegrationTestBase.launch {}
        }
        every { lunchmoneyMock.getBudgetSummary(any(), any(), any()) } returns Mono.just(listOf())
        excludeRecords { lunchmoneyMock.getBudgetSummary(any(), any(), any()) }
        every {
            periodicFetcherFactory.create<List<LunchmoneyCategory>>(
                "Lunchmoney categories",
                any(),
            )
        } returns categoriesFetcherMock
        coEvery {
            categoriesFetcherMock.fetched()
        } returns TestData.categories
        coEvery { budgetSettingsVerifier.verify() } just runs

        app.setupKoinModules(
            this@IntegrationTestBase,
            InMemoryStatementRetryRepository(),
            Settings.load(
                Paths.get(
                    TransactionsTest::class.java.classLoader.getResource("test-settings.yml")!!.path,
                ),
            ),
            MonoWebhookSettings(false, URI.create(""), 0),
        )
        app.modules(
            module {
                single { lunchmoneyMock }
                single { webhookListener } bind MonoWebhookListener::class bind StatementSource::class
                single { InMemoryStatementRetryRepository() } bind StatementRetryRepository::class
                single { tgMock }
                single { budgetSettingsVerifier } bind ApplicationStartupVerifier::class
                single { periodicFetcherFactory }
            },
        )
    }

    protected fun setupTransferTransactionMocks(
        isFirst: (LunchmoneyInsertTransaction) -> Boolean,
    ): Pair<Long, Long> = setupTransferTransactionMocks(emptyList(), isFirst)

    protected fun setupTransferTransactionMocks(
        fails: List<IntegrationFailConfig>,
        isFirst: (LunchmoneyInsertTransaction) -> Boolean,
    ): Pair<Long, Long> {
        var insertTransaction: LunchmoneyInsertTransaction? = null
        var insertTransaction2: LunchmoneyInsertTransaction? = null
        val newTransactionId = 1L
        val newTransactionId2 = 2L
        val trGroupId = 3L
        val insertTracker =
            FailTrackerTransformation<List<Long>>(fails.filterIsInstance<IntegrationFailConfig.Insert>())
        every { lunchmoneyMock.insertTransactions(any(), any(), any(), any(), any(), any()) } answers {
            val i = firstArg<List<LunchmoneyInsertTransaction>>()[0]
            val mono =
                if (isFirst(i)) {
                    insertTransaction = i
                    Mono.just(listOf(newTransactionId))
                } else {
                    insertTransaction2 = i
                    Mono.just(listOf(newTransactionId2))
                }
            mono.transformDeferred(insertTracker)
        }
        val singleTransform =
            FailTrackerTransformation<LunchmoneyTransaction>(
                fails.filterIsInstance<IntegrationFailConfig.GetSingle>(),
            )
        every { lunchmoneyMock.getSingleTransaction(newTransactionId, any()) } answers {
            Mono.just(
                LunchmoneyTransaction(
                    id = newTransactionId,
                    isGroup = false,
                    date = insertTransaction!!.date,
                    payee = insertTransaction!!.payee!!,
                    amount = insertTransaction!!.amount,
                    currency = insertTransaction!!.currency!!,
                    toBase = 1.0,
                    notes = insertTransaction?.notes,
                    categoryId = insertTransaction?.categoryId,
                    status = insertTransaction!!.status!!,
                    accountDisplayName = "",
                    createdAt = Clock.System.now().toJavaInstant(),
                    updatedAt = Clock.System.now().toJavaInstant(),
                    displayName = "",
                    excludeFromTotals = false,
                    excludeFromBudget = false,
                    isIncome = false,
                    isPending = false,
                ),
            ).transformDeferred(singleTransform)
        }
        every { lunchmoneyMock.getSingleTransaction(newTransactionId2, any()) } answers {
            Mono.just(
                LunchmoneyTransaction(
                    id = newTransactionId2,
                    isGroup = false,
                    date = insertTransaction2!!.date,
                    payee = insertTransaction2!!.payee!!,
                    amount = insertTransaction2!!.amount,
                    currency = insertTransaction2!!.currency!!,
                    toBase = 1.0,
                    notes = insertTransaction2?.notes,
                    categoryId = insertTransaction2?.categoryId,
                    status = insertTransaction2!!.status!!,
                    accountDisplayName = "",
                    createdAt = Clock.System.now().toJavaInstant(),
                    updatedAt = Clock.System.now().toJavaInstant(),
                    displayName = "",
                    excludeFromTotals = false,
                    excludeFromBudget = false,
                    isIncome = false,
                    isPending = false,
                ),
            ).transformDeferred(singleTransform)
        }
        val updateTracker =
            FailTrackerTransformation<LunchmoneyUpdateTransactionResponse>(
                fails.filterIsInstance<IntegrationFailConfig.Update>(),
            )
        every {
            lunchmoneyMock.updateTransaction(any(), any(), any(), any(), any())
        } answers {
            Mono.just(mockk<LunchmoneyUpdateTransactionResponse>())
                .transformDeferred(updateTracker)
        }
        val createTransactionGroupTracker =
            FailTrackerTransformation<Long>(
                fails.filterIsInstance<IntegrationFailConfig.CreateTransactionGroup>(),
            )
        every {
            lunchmoneyMock.createTransactionGroup(any(), any(), any(), any(), any(), any())
        } answers {
            Mono.just(trGroupId)
                .transformDeferred(createTransactionGroupTracker)
        }

        return Pair(newTransactionId, newTransactionId2)
    }

    protected fun runTestApplication(block: suspend () -> Unit) {
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

    protected fun setupSingleTransactionMocks(fails: List<IntegrationFailConfig> = emptyList()): Long {
        var insertTransaction: LunchmoneyInsertTransaction? = null
        val newTransactionId = 1L

        val insertTracker =
            FailTrackerTransformation<List<Long>>(fails.filterIsInstance<IntegrationFailConfig.Insert>())
        every { lunchmoneyMock.insertTransactions(any(), any(), any(), any(), any(), any()) } answers {
            insertTransaction = firstArg<List<LunchmoneyInsertTransaction>>()[0]
            Mono.just(listOf(newTransactionId))
                .transformDeferred(insertTracker)
        }
        val singleTracker =
            FailTrackerTransformation<LunchmoneyTransaction>(
                fails.filterIsInstance<IntegrationFailConfig.GetSingle>(),
            )
        every { lunchmoneyMock.getSingleTransaction(newTransactionId, any()) } answers {
            Mono.just(
                LunchmoneyTransaction(
                    id = newTransactionId,
                    isGroup = false,
                    date = insertTransaction!!.date,
                    payee = insertTransaction!!.payee!!,
                    amount = insertTransaction!!.amount,
                    currency = insertTransaction!!.currency!!,
                    toBase = 1.0,
                    notes = insertTransaction?.notes,
                    categoryId = insertTransaction?.categoryId,
                    status = insertTransaction!!.status!!,
                    accountDisplayName = "",
                    createdAt = Clock.System.now().toJavaInstant(),
                    updatedAt = Clock.System.now().toJavaInstant(),
                    displayName = "",
                    excludeFromTotals = false,
                    excludeFromBudget = false,
                    isIncome = false,
                    isPending = false,
                ),
            ).transformDeferred(singleTracker)
        }
        return newTransactionId
    }
}
