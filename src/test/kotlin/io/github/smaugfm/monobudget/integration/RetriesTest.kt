package io.github.smaugfm.monobudget.integration

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.integration.TestData.UAH
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.verifySequence
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@Suppress("LongMethod")
class RetriesTest : IntegrationTestBase() {
    @Test
    fun `When lunchmoney fails and then recovers transfer transaction is processed correctly`() {
        val (newTransactionId, newTransactionId2) =
            setupTransferMocks(
                listOf(
                    IntegrationFailConfig.CreateTransactionGroup(0..0),
                ),
            ) { it.amount < BigDecimal.ZERO }

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d =
                            MonoWebhookResponseData(
                                account = "MONO-EXAMPLE-UAH2",
                                statementItem =
                                    MonoStatementItem(
                                        id = UUID.randomUUID().toString(),
                                        time = Clock.System.now(),
                                        description = "test send",
                                        mcc = 4829,
                                        originalMcc = 4829,
                                        hold = true,
                                        amount = -5600,
                                        operationAmount = -5600,
                                        currencyCode = UAH,
                                        commissionRate = 0,
                                        cashbackAmount = 0,
                                        balance = 0,
                                    ),
                            ),
                        accountCurrency = UAH,
                    ),
                ),
            )
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d =
                            MonoWebhookResponseData(
                                account = "MONO-EXAMPLE-UAH",
                                statementItem =
                                    MonoStatementItem(
                                        id = UUID.randomUUID().toString(),
                                        time = Clock.System.now(),
                                        description = "test receive",
                                        mcc = 4829,
                                        originalMcc = 4829,
                                        hold = true,
                                        amount = 5600,
                                        operationAmount = 5600,
                                        currencyCode = UAH,
                                        commissionRate = 0,
                                        cashbackAmount = 0,
                                        balance = 0,
                                    ),
                            ),
                        accountCurrency = UAH,
                    ),
                ),
            )

            coVerify(timeout = 1000, exactly = 1) {
                tgMock.sendMessage(
                    match {
                        it is ChatId.IntegerId && it.id == 55555556L
                    },
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
            coVerify(timeout = 1000, exactly = 2) {
                tgMock.sendMessage(
                    match {
                        it is ChatId.IntegerId && it.id == 55555555L
                    },
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
            verifySequence {
                lunchmoneyMock.insertTransactions(any(), any(), any(), any(), any(), any())
                lunchmoneyMock.getSingleTransaction(eq(newTransactionId), any())
                lunchmoneyMock.updateTransaction(eq(newTransactionId), any(), any(), any(), any())
                lunchmoneyMock.insertTransactions(any(), any(), any(), any(), any(), any())
                lunchmoneyMock.getSingleTransaction(eq(newTransactionId2), any())
                lunchmoneyMock.createTransactionGroup(
                    any(),
                    "Transfer",
                    listOf(newTransactionId, newTransactionId2),
                    444444L,
                    any(),
                    any(),
                )
                lunchmoneyMock.createTransactionGroup(
                    any(),
                    "Transfer",
                    listOf(newTransactionId, newTransactionId2),
                    444444L,
                    any(),
                    any(),
                )
            }
            confirmVerified(lunchmoneyMock)
        }
    }

    @Test
    fun `When lunchmoney fails and then recovers successful method calls are not retried`() {
        val newTransactionId =
            setupSingleTransactionMocks(
                listOf(
                    IntegrationFailConfig.GetSingle(0..0),
                ),
            )

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d =
                            MonoWebhookResponseData(
                                account = "MONO-EXAMPLE-UAH",
                                statementItem =
                                    MonoStatementItem(
                                        id = UUID.randomUUID().toString(),
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
                                    ),
                            ),
                        accountCurrency = UAH,
                    ),
                ),
            )

            coVerify(timeout = 1000, exactly = 2) {
                tgMock.sendMessage(
                    match {
                        it is ChatId.IntegerId && it.id == 55555555L
                    },
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }

            verifySequence {
                lunchmoneyMock.insertTransactions(any(), any(), any(), any(), any(), any())
                lunchmoneyMock.getSingleTransaction(eq(newTransactionId), any())
                lunchmoneyMock.getSingleTransaction(eq(newTransactionId), any())
            }
            confirmVerified(lunchmoneyMock)
        }
    }
}
