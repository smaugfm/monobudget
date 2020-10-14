import assertk.assertThat
import assertk.assertions.containsExactly
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.github.smaugfm.events.Event
import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
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
        val transactionId = "vasa"

        coEvery { api.sendMessage(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { mappings.getTelegramChatIdAccByMono(any()) } returns chatId

        runBlocking {

            val statementItem = mockk<MonoStatementItem>()
            every { statementItem.time } returns Clock.System.now() - 2.days
            every { statementItem.amount } returns -11500
            every { statementItem.mcc } returns 5722
            every { statementItem.description } returns "Rozetka"
            every { statementItem.currencyCode } returns Currency.getInstance("UAH")

            val monoResponse = mockk<MonoWebHookResponseData>()
            every { monoResponse.account } returns monoAccount
            every { monoResponse.statementItem } returns statementItem

            val transaction = mockk<YnabTransactionDetail>()
            every { transaction.payee_name } returns "rozetka"
            every { transaction.category_name } returns "Продукты/быт"
            every { transaction.id } returns transactionId

            telegram.sendStatementMessage(
                monoResponse,
                transaction
            )
        }

        val data = TelegramHandler.callbackData(transactionId)
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
                                "Unclear",
                                callback_data = data(TelegramHandler.Companion.UpdateType.Unclear)
                            ),
                            InlineKeyboardButton(
                                "Mark red",
                                callback_data = data(TelegramHandler.Companion.UpdateType.MarkRed)
                            ),
                            InlineKeyboardButton(
                                "Невыясненные",
                                callback_data = data(TelegramHandler.Companion.UpdateType.Unrecognized)
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

        runBlocking {
            telegram.handleCallbackQuery(
                dispatchedEvents::add,
                chatId,
                "vasa"
            )
        }

        runBlocking {
            telegram.handleCallbackQuery(
                dispatchedEvents::add,
                chatId,
                "vasa   vasa"
            )
        }

        coVerify {
            api.sendMessage(chatId, TelegramHandler.unknownErrorMessage)
            api.sendMessage(chatId, TelegramHandler.unknownErrorMessage)
        }

        val transactionId = "vasa"
        val type = TelegramHandler.Companion.UpdateType.MarkRed
        runBlocking {
            telegram.handleCallbackQuery(
                dispatchedEvents::add,
                chatId,
                "$type   $transactionId"
            )
        }

        assertThat(dispatchedEvents).containsExactly(Event.Ynab.UpdateTransaction(transactionId, type))
    }
}
