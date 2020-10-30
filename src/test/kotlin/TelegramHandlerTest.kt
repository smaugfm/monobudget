import assertk.assertThat
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
import java.util.UUID
import kotlin.time.days

class TelegramHandlerTest {
    val chatId = 0
    val api = mockk<TelegramApi>()
    val mappings = mockk<Mappings>()
    val telegram = TelegramHandler(api, mappings)

    private fun getMonoResponseAndTransaction(
        payee: String,
        monoAccount: String
    ): Pair<MonoWebHookResponseData, YnabTransactionDetail> {
        val statementItem = mockk<MonoStatementItem>()
        every { statementItem.time } returns Clock.System.now() - 2.days
        every { statementItem.amount } returns -11500
        every { statementItem.mcc } returns 5722
        every { statementItem.description } returns payee
        every { statementItem.currencyCode } returns Currency.getInstance("UAH")

        val monoResponse = mockk<MonoWebHookResponseData>()
        every { monoResponse.account } returns monoAccount
        every { monoResponse.statementItem } returns statementItem

        val transaction = mockk<YnabTransactionDetail>()
        every { transaction.payee_name } returns payee
        every { transaction.category_name } returns "Продукты/быт"
        every { transaction.id } returns "id"

        return Pair(monoResponse, transaction)
    }

    @Test
    fun `Send statement message`() {
        val monoAccount = "vasility"
        val payee = "Rozetka"
        coEvery { api.sendMessage(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { mappings.getTelegramChatIdAccByMono(any()) } returns chatId

        val (monoResponse, transaction) =
            getMonoResponseAndTransaction(payee, monoAccount)

        runBlocking {

            telegram.sendStatementMessage(
                Event.Telegram.SendStatementMessage(monoResponse, transaction)
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
                                callback_data = TransactionActionType.Uncategorize.serialize()
                            ),
                            InlineKeyboardButton(
                                "\uD83D\uDEABunapprove",
                                callback_data = TransactionActionType.Unapprove.serialize()
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                "➡️Невыясненные",
                                callback_data = TransactionActionType.Unknown.serialize()
                            ),
                            InlineKeyboardButton(
                                "➕payee",
                                callback_data = TransactionActionType.MakePayee.serialize()
                            ),
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `Test extractPayeeAndTransactionIdFromMessage`() {
        val payee = "Галинка Савченко"
        val (monoResponse, transaction) =
            getMonoResponseAndTransaction(payee, "vasa")
        val id = UUID.randomUUID().toString()

        val message = telegram.formatHTMLStatementMessage(monoResponse.statementItem, transaction, id)
        val replaceHtml = Regex("<.*?>")
        val adjustedMessage = replaceHtml.replace(message, "")
        val (extractedPayee, extractedId) = telegram.extractPayeeAndTransactionIdFromMessage(adjustedMessage)

        assertThat(extractedPayee).isEqualTo(payee)
        assertThat(extractedId).isEqualTo(id)
    }
}
