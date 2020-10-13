import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.ynab.YnabTransactionDetail
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.time.days

class PlaygroundTest {
    @Test
    fun test() {
        runBlocking {
            val settings = Settings.loadDefault()
            val telegram =
                TelegramHandler(
                    TelegramApi.create(settings.telegramBotUsername, settings.telegramBotToken),
                    settings.mappings,
                )

            val statementItem = mockk<MonoStatementItem>()
            every { statementItem.time } returns Clock.System.now() - 2.days
            every { statementItem.amount } returns -11500
            every { statementItem.mcc } returns 5722
            every { statementItem.description } returns "Rozetka"
            every { statementItem.currencyCode } returns Currency.getInstance("UAH")

            val monoResponse = mockk<MonoWebHookResponseData>()
            every { monoResponse.account } returns settings.mappings.getMonoAccounts().find { it.startsWith("p") }!!
            every { monoResponse.statementItem } returns statementItem

            val transaction = mockk<YnabTransactionDetail>()
            every { transaction.payee_name } returns "rozetka"
            every { transaction.category_name } returns "Продукты/быт"
            every { transaction.id } returns "12342"

            telegram.sendStatementMessage(
                monoResponse,
                transaction)
        }
    }
}