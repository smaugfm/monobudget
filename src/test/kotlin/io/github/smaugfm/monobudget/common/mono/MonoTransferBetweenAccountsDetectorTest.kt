package io.github.smaugfm.monobudget.common.mono

import assertk.assertThat
import assertk.assertions.isInstanceOf
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.TestBase
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.MaybeTransferStatement.NotTransfer
import io.github.smaugfm.monobudget.common.account.MaybeTransferStatement.Transfer
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.misc.ConcurrentExpiringMap
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.koin.core.KoinApplication
import org.koin.dsl.module
import org.koin.test.mock.declareMock
import java.util.Currency
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

private val log = KotlinLogging.logger { }

class MonoTransferBetweenAccountsDetectorTest : TestBase() {
    companion object {
        private val cache = ConcurrentExpiringMap<StatementItem, Deferred<Any>>(1.minutes)
    }

    class TestDetector(
        bankAccounts: BankAccountService,
        ctx: StatementProcessingContext,
    ) : TransferBetweenAccountsDetector<Any>(
        bankAccounts,
        ctx,
        cache,
    )

    override fun KoinApplication.testKoinApplication() {
        modules(
            module {
                scope<StatementProcessingScopeComponent> {
                    scoped { TestDetector(get(), get()) }
                }
            },
        )
    }

    @Timeout(2, unit = TimeUnit.SECONDS)
    @Test
    fun test() {
        val ctx1 = StatementProcessingContext(statementItem1())
        val ctx2 = StatementProcessingContext(statementItem2())

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
                    assertThat(detector.checkTransfer()).isInstanceOf(NotTransfer::class)
                    val notTransfer = detector.checkTransfer() as NotTransfer
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
                    assertThat(detector.checkTransfer()).isInstanceOf(Transfer::class)
                    sc.scope.close()
                }
            job1.join()
            job2.join()
        }
    }
}
