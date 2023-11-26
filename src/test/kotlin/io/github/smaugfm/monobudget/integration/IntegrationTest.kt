package io.github.smaugfm.monobudget.integration

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.integration.TestData.UAH
import io.github.smaugfm.monobudget.mono.MonobankWebhookResponseStatementItem
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.UUID

@Suppress("LongMethod")
class IntegrationTest : IntegrationTestBase(), CoroutineScope {
    @Test
    fun `When nothing happens finishes normally`() {
        runTestApplication {
            delay(100)
        }
    }

    @Test
    fun `Other account transaction triggers transfer`() {
        val (newTransactionId, newTransactionId2) = setupTransferMocks { it.amount > BigDecimal.ZERO }

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d =
                            MonoWebhookResponseData(
                                account = "MONO-EXAMPLE-UAH",
                                statementItem =
                                    MonoStatementItem(
                                        id = UUID.randomUUID().toStr(),
                                        time = Clock.System.now(),
                                        description = "Від: 777777****1234",
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
            }
            confirmVerified(lunchmoneyMock)
        }
    }

    @Test
    fun `Mono transfer triggers single and transfer transaction creation`() {
        val (newTransactionId, newTransactionId2) = setupTransferMocks { it.amount < BigDecimal.ZERO }

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d =
                            MonoWebhookResponseData(
                                account = "MONO-EXAMPLE-UAH2",
                                statementItem =
                                    MonoStatementItem(
                                        id = UUID.randomUUID().toStr(),
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
                                        id = UUID.randomUUID().toStr(),
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
                        it is ChatId.IntegerId && it.id == 55555555L
                    },
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
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
            }
            confirmVerified(lunchmoneyMock)
        }
    }

    @Test
    fun `Mono webhook triggers new transaction creation`() {
        var insertTransaction: LunchmoneyInsertTransaction? = null
        val newTransactionId = 1L
        every { lunchmoneyMock.insertTransactions(any(), any(), any(), any(), any(), any()) } answers {
            insertTransaction = firstArg<List<LunchmoneyInsertTransaction>>()[0]
            Mono.just(listOf(newTransactionId))
        }
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
                ),
            )
        }

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    MonobankWebhookResponseStatementItem(
                        d =
                            MonoWebhookResponseData(
                                account = "MONO-EXAMPLE-UAH",
                                statementItem =
                                    MonoStatementItem(
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
                                    ),
                            ),
                        accountCurrency = UAH,
                    ),
                ),
            )
            coVerify(timeout = 1000, exactly = 1) {
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
            }
            confirmVerified(lunchmoneyMock)
        }
    }
}
