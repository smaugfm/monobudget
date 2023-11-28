package io.github.smaugfm.monobudget.integration

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.TestData.exampleStatement1
import io.github.smaugfm.monobudget.TestData.exampleStatement2
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.integration.util.IntegrationTestBase
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@Suppress("LongMethod")
class TransactionsTest : IntegrationTestBase(), CoroutineScope {
    @Test
    fun `When nothing happens finishes normally`() {
        runTestApplication {
            delay(100)
        }
    }

    @Test
    fun `Other account transaction triggers transfer`() {
        val (newTransactionId, newTransactionId2) =
            setupTransferTransactionMocks { it.amount > BigDecimal.ZERO }

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(
                    exampleStatement1("Від: 777777****1234"),
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
        val (newTransactionId, newTransactionId2) =
            setupTransferTransactionMocks { it.amount < BigDecimal.ZERO }

        runTestApplication {
            webhookStatementsFlow.emit(
                StatementProcessingContext(exampleStatement2("test send")),
            )
            webhookStatementsFlow.emit(
                StatementProcessingContext(exampleStatement1("test receive")),
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
        val newTransactionId = setupSingleTransactionMocks()

        runTestApplication {
            webhookStatementsFlow.emit(StatementProcessingContext(exampleStatement2("test")))
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
            }
            confirmVerified(lunchmoneyMock)
        }
    }
}
