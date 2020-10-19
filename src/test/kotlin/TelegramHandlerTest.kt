import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.telegram.TelegramHandler
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.ynab.YnabTransactionDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.util.Currency
import kotlin.time.days

class TelegramHandlerTest {
    val chatId = 0
    val api = mockk<TelegramApi>()
    val mappings = mockk<Mappings>()
    val telegram = TelegramHandler(api, mappings)

    @Test
    fun `Send statement message`() {
        val monoAccount = "vasa"
        val id = "vasa"
        val description = "Rozetka"

        coEvery { api.sendMessage(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { mappings.getTelegramChatIdAccByMono(any()) } returns chatId

        runBlocking {

            val statementItem = mockk<MonoStatementItem>()
            every { statementItem.time } returns Clock.System.now() - 2.days
            every { statementItem.amount } returns -11500
            every { statementItem.mcc } returns 5722
            every { statementItem.description } returns description
            every { statementItem.currencyCode } returns Currency.getInstance("UAH")

            val monoResponse = mockk<MonoWebHookResponseData>()
            every { monoResponse.account } returns monoAccount
            every { monoResponse.statementItem } returns statementItem

            val transaction = mockk<YnabTransactionDetail>()
            every { transaction.payee_name } returns "Rozetka"
            every { transaction.category_name } returns "Продукты/быт"
            every { transaction.id } returns id

            telegram.sendStatementMessage(
                monoResponse,
                transaction
            )
        }

        coVerify {
            mappings.getTelegramChatIdAccByMono(monoAccount)
            api.sendMessage(
                chatId,
                any(),
                "HTML",
                null,
                null,
                null,
                InlineKeyboardMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton(
                                "❌категорию",
                                callback_data = TransactionActionType.Uncategorize(id).serialize()
                            ),
                            InlineKeyboardButton(
                                "❌payee",
                                callback_data = TransactionActionType.Unpayee(id).serialize()
                            ),
                            InlineKeyboardButton(
                                "\uD83D\uDEABreject",
                                callback_data = TransactionActionType.Unapprove(id).serialize()
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                "➡️Невыясненные",
                                callback_data = TransactionActionType.Unknown(id).serialize()
                            ),
                            InlineKeyboardButton(
                                "➕payee",
                                callback_data = TransactionActionType.MakePayee(id, description).serialize()
                            ),
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `Test callback handler, error message is sent`() {
        val dispatchedEvents = mutableListOf<Event>()

        coEvery { api.sendMessage(any(), any()) } returns Unit

        val result1 = runBlocking {
            telegram.handleCallbackQuery(
                dispatchedEvents::add,
                "vasa"
            )
        }

        val result2 = runBlocking {
            telegram.handleCallbackQuery(
                dispatchedEvents::add,
                "vasa   vasa"
            )
        }

        assertThat(result1).isEqualTo(TelegramHandler.unknownErrorMessage)
        assertThat(result2).isEqualTo(TelegramHandler.unknownErrorMessage)

        val transactionId = "vasa"
        val payee = "victor"
        val type = TransactionActionType.MakePayee(transactionId, payee)
        val result3 = runBlocking {
            telegram.handleCallbackQuery(
                dispatchedEvents::add,
                "${TransactionActionType.MakePayee::class.simpleName}   $transactionId   ($payee)"
            )
        }

        assertThat(result3).isEqualTo(null)

        val serialized = type.serialize()
        val deserialized = TransactionActionType.deserialize(serialized)

        assertThat(dispatchedEvents).containsExactly(Event.Ynab.TransactionAction(type))
        assertThat(deserialized).isEqualTo(type)
    }
}
