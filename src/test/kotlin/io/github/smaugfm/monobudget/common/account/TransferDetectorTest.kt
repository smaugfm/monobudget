package io.github.smaugfm.monobudget.common.account

import assertk.assertThat
import assertk.assertions.isInstanceOf
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.TestBase
import io.github.smaugfm.monobudget.common.account.MaybeTransfer.NotTransfer
import io.github.smaugfm.monobudget.common.account.MaybeTransfer.Transfer
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.util.misc.ConcurrentExpiringMap
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.test.mock.declareMock
import java.util.Currency
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

private val log = KotlinLogging.logger { }

class TransferDetectorTest : TestBase() {
    companion object {
        private val cache = ConcurrentExpiringMap<StatementItem, Deferred<Any>>(1.minutes)
    }

    class TestDetector(
        bankAccounts: BankAccountService,
        ctx: StatementProcessingContext,
    ) : TransferDetector<Any>(
            bankAccounts,
            ctx,
            cache,
        )

    override fun testKoinApplication(app: KoinApplication) {
        app.modules(
            module {
                scope<StatementProcessingScopeComponent> {
                    scoped { TestDetector(get(), get()) }
                }
            },
        )
    }

    @Timeout(2, unit = TimeUnit.SECONDS)
    @Test
    fun `Detects a transfer`() {
        val ctx1 = StatementProcessingContext(transferStatement1)
        val ctx2 = StatementProcessingContext(transferStatement2)

        declareMock<BankAccountService> {
            coEvery { getAccountCurrency(ctx1.item.accountId) } returns Currency.getInstance("UAH")
            coEvery { getAccountCurrency(ctx2.item.accountId) } returns Currency.getInstance("USD")
        }
        runBlocking {
            val waitForNotTransfer = CompletableDeferred<Any>()
            val job1 =
                launch {
                    val sc = StatementProcessingScopeComponent(ctx1)
                    val detector = sc.scope.get<TestDetector>()
                    assertThat(detector.checkForTransfer()).isInstanceOf(NotTransfer::class)
                    val notTransfer = detector.checkForTransfer() as NotTransfer
                    waitForNotTransfer.complete(Any())
                    notTransfer.consume {
                        log.info { "Started consuming transaction 1..." }
                        delay(100)
                        log.info { "Finished consuming transaction 1" }
                    }
                    sc.scope.close()
                }

            waitForNotTransfer.await()
            val job2 =
                launch {
                    val sc = StatementProcessingScopeComponent(ctx2)
                    val detector = sc.scope.get<TestDetector>()
                    assertThat(detector.checkForTransfer()).isInstanceOf(Transfer::class)
                    sc.scope.close()
                }
            job1.join()
            job2.join()
        }
    }

    private val transferStatement1 =
        MonobankWebhookResponseStatementItem(
            MonoWebhookResponseData(
                "acc1",
                MonoStatementItem(
                    "aaa",
                    Instant.parse("2023-04-02T18:12:41Z"),
                    "З доларової картки",
                    4829,
                    4829,
                    true,
                    3665,
                    100,
                    Currency.getInstance("USD"),
                    0,
                    0,
                    1234,
                ),
            ),
            Currency.getInstance("UAH"),
        )

    private val transferStatement2 =
        MonobankWebhookResponseStatementItem(
            MonoWebhookResponseData(
                "acc2",
                MonoStatementItem(
                    "bbb",
                    Instant.parse("2023-04-02T18:12:42Z"),
                    "Переказ на картку",
                    4829,
                    4829,
                    true,
                    -100,
                    -3665,
                    Currency.getInstance("UAH"),
                    0,
                    0,
                    1234,
                ),
            ),
            Currency.getInstance("UAH"),
        )
}
