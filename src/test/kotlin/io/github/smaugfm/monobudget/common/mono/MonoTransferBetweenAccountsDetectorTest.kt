package io.github.smaugfm.monobudget.common.mono

import assertk.assertThat
import assertk.assertions.isInstanceOf
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.Base
import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector.MaybeTransfer.NotTransfer
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector.MaybeTransfer.Transfer
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.mock.declareMock
import java.util.Currency
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

class MonoTransferBetweenAccountsDetectorTest : Base() {

    class TestDetector : TransferBetweenAccountsDetector<Any>()

    val detector: TestDetector by inject()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                single { TestDetector() }
            }
        )
    }

    @Timeout(2, unit = TimeUnit.SECONDS)
    @Test
    fun test() {
        val webhook1 = webhook1()
        val webhook2 = webhook2()

        declareMock<AccountsService> {
            coEvery { getAccountCurrency(webhook1.accountId) } returns Currency.getInstance("UAH")
            coEvery { getAccountCurrency(webhook2.accountId) } returns Currency.getInstance("USD")
        }
        runBlocking {
            val waitForNotTransfer = CompletableDeferred<Any>()
            val job1 = launch {
                assertThat(detector.checkTransfer(webhook1)).isInstanceOf(NotTransfer::class)
                val notTransfer = detector.checkTransfer(webhook1) as NotTransfer
                waitForNotTransfer.complete(Any())
                notTransfer.consume {
                    log.info { "Started consuming transaction 1..." }
                    delay(100)
                    log.info { "Finished consuming transaction 1" }
                }
            }

            waitForNotTransfer.await()
            val job2 = launch {
                assertThat(detector.checkTransfer(webhook2)).isInstanceOf(Transfer::class)
            }
            job1.join()
            job2.join()
        }
    }

    private fun webhook1() = MonobankWebhookResponseStatementItem(
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
                1234
            )
        )
    )

    private fun webhook2() = MonobankWebhookResponseStatementItem(
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
                1234
            )
        )
    )
}
