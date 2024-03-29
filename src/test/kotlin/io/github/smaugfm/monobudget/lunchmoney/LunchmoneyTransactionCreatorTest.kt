package io.github.smaugfm.monobudget.lunchmoney

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.lunchmoney.response.LunchmoneyUpdateTransactionResponse
import io.github.smaugfm.monobudget.TestBase
import io.github.smaugfm.monobudget.TestData.exampleStatement1
import io.github.smaugfm.monobudget.common.account.MaybeTransfer
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.retry.InMemoryStatementRetryRepository
import io.github.smaugfm.monobudget.common.retry.StatementRetryRepository
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinApplication
import org.koin.core.component.get
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.mock.declareMock
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

class LunchmoneyTransactionCreatorTest : TestBase() {
    override fun testKoinApplication(app: KoinApplication) {
        app.modules(
            module {
                single { InMemoryStatementRetryRepository() } bind StatementRetryRepository::class
                scope<StatementProcessingScopeComponent> {
                    scoped {
                        LunchmoneyTransactionCreator(get(), get(), get())
                    }
                }
            },
        )
    }

    private val expectedTransactionGroupId = 2L
    private val expectedTransactionCreatedId = 1L
    private val expectedTransaction =
        LunchmoneyTransaction(
            1,
            LocalDate.MIN,
            "payee",
            BigDecimal.TEN,
            Currency.getInstance("UAH"),
            1.0,
            isGroup = false,
            status = LunchmoneyTransactionStatus.CLEARED,
            accountDisplayName = "",
            createdAt = Clock.System.now().toJavaInstant(),
            updatedAt = Clock.System.now().toJavaInstant(),
            displayName = "",
            excludeFromTotals = false,
            excludeFromBudget = false,
            isIncome = false,
            isPending = false,
        )
    private val insertTransaction =
        LunchmoneyInsertTransaction(
            LocalDate.MIN,
            BigDecimal.TEN,
        )

    @BeforeEach()
    fun mocks() {
        declareMock<BudgetBackend.Lunchmoney> {
            every { transferCategoryId } returns "1"
        }
        declareMock<LunchmoneyNewTransactionFactory>(secondaryTypes = listOf(NewTransactionFactory::class)) {
            coEvery { create(any()) } returns insertTransaction
        }
    }

    @Test
    fun noApiCallsWhenContextPresent() {
        declareMock<LunchmoneyApi> {}
        runBlocking {
            val map = mutableMapOf<String, Any>()
            val ctx = StatementProcessingContext(exampleStatement1("test"), map)
            map["transactionUpdated"] = true
            map["transactionCreatedId"] = expectedTransactionCreatedId
            map["transaction"] = expectedTransaction
            map["transactionGroupId"] = expectedTransactionGroupId
            val sc = StatementProcessingScopeComponent(ctx)
            val creator = sc.get<LunchmoneyTransactionCreator>()
            assertThat(
                creator.create(
                    MaybeTransfer.Transfer(
                        sc.ctx.item,
                        expectedTransaction,
                    ),
                ),
            ).isEqualTo(expectedTransaction)
        }
    }

    @Test
    fun onlyNecessaryApiCallsWhenContextPartiallyPopulated() {
        declareMock<LunchmoneyApi> {
            every { createTransactionGroup(any(), any(), any(), any(), any()) } returns
                Mono.just(
                    expectedTransactionGroupId,
                )
        }
        runBlocking {
            val map = mutableMapOf<String, Any>()
            val ctx = StatementProcessingContext(exampleStatement1("test"), map)
            map["transactionUpdated"] = true
            map["transactionCreatedId"] = expectedTransactionCreatedId
            map["transaction"] = expectedTransaction
            val sc = StatementProcessingScopeComponent(ctx)
            val creator = sc.get<LunchmoneyTransactionCreator>()
            assertThat(
                creator.create(
                    MaybeTransfer.Transfer(
                        sc.ctx.item,
                        expectedTransaction,
                    ),
                ),
            ).isEqualTo(expectedTransaction)
            assertThat(map["transactionGroupId"]).isEqualTo(expectedTransactionGroupId)
        }
    }

    @Test
    fun contextGetsPopulated() {
        declareMock<LunchmoneyApi> {
            every { updateTransaction(any(), any(), any()) } returns
                Mono.just(
                    mockkClass(LunchmoneyUpdateTransactionResponse::class),
                )
            every { insertTransactions(any(), any(), any(), any(), any(), any()) } returns
                Mono.just(
                    listOf(
                        expectedTransactionCreatedId,
                    ),
                )
            every { getSingleTransaction(any(), any()) } returns Mono.just(expectedTransaction)
            every { createTransactionGroup(any(), any(), any(), any(), any()) } returns
                Mono.just(
                    expectedTransactionGroupId,
                )
        }
        runBlocking {
            val map = mutableMapOf<String, Any>()
            val sc =
                StatementProcessingScopeComponent(
                    StatementProcessingContext(
                        exampleStatement1("test"),
                        map,
                    ),
                )
            val creator = sc.get<LunchmoneyTransactionCreator>()
            assertThat(
                creator.create(
                    MaybeTransfer.Transfer(
                        sc.ctx.item,
                        expectedTransaction,
                    ),
                ),
            ).isEqualTo(expectedTransaction)
            assertThat(map["transactionUpdated"]).isEqualTo(true)
            assertThat(map["transactionGroupId"]).isEqualTo(expectedTransactionGroupId)
            assertThat(map["transactionCreatedId"]).isEqualTo(expectedTransactionCreatedId)
            assertThat(map["transaction"]).isEqualTo(
                expectedTransaction,
            )
        }
    }
}
