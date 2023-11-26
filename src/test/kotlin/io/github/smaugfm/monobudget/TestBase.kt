package io.github.smaugfm.monobudget

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.KoinApplication
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.mock.MockProvider
import java.util.Currency

@ExtendWith(MockKExtension::class)
@Suppress("MagicNumber")
open class TestBase : KoinTest {
    open fun testKoinApplication(app: KoinApplication) {
    }

    @Suppress("unused")
    @JvmField
    @RegisterExtension
    val koinTestExtension =
        KoinTestExtension.create {
            testKoinApplication(this@create)
        }

    @BeforeEach
    fun declareMockProvider() {
        MockProvider.register { mockkClass(it) }
    }

    companion object {
        fun statementItem1() =
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

        fun statementItem2() =
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
}
