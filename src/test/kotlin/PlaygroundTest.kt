import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.ynab.YnabSaveTransaction
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.util.*
import kotlin.time.days

class PlaygroundTest {
    @Test
    fun test() {
        runBlocking {
            val settings = Settings.load(Paths.get("settings.json"))
            val telegram =
                TelegramHandler(
                    TelegramApi.create(settings.telegramBotUsername, settings.telegramBotToken)
                )

            val statementItem = mockk<MonoStatementItem>()
            every { statementItem.time } returns Clock.System.now() - 2.days
            every { statementItem.amount } returns -11500
            every { statementItem.mcc } returns 5722
            every { statementItem.description } returns "Rozetka"
            every { statementItem.currencyCode } returns Currency.getInstance("UAH")

            val transaction = mockk<YnabSaveTransaction>()
            every { transaction.payee_name } returns "rozetka"

            telegram.sendStatementMessage(settings.monoAcc2Telegram.values.find { it.toString().startsWith("4") }!!,
                statementItem,
                transaction,
                "Продукты/быт")
        }
    }
}