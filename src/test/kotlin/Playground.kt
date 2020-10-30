import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.telegram.TelegramHandler
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.PayeeSuggestor
import com.github.smaugfm.ynab.YnabApi
import com.github.smaugfm.ynab.YnabTransactionDetail
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.UUID
import kotlin.time.days

class Playground {
    @Test
    @Disabled
    fun `Get last 29 days Mono transactions`() {
        runBlocking {
            val window = 30
            val untilDays = window * 7

            val settings = Settings.loadDefault()
            val mono = MonoApi(settings.monoTokens.drop(1).first())
            val ynab = YnabApi(settings.ynabToken, settings.ynabBudgetId)
            val statementItems = mono.fetchStatementItems(
                settings.mappings.getMonoAccounts().find { it.startsWith("3") }!!,
                Clock.System.now() - (window + untilDays).days,
                Clock.System.now() - untilDays.days,
            )

            val payees = ynab.getPayees()
            val payeeNames = payees.map { it.name }
            val suggestor = PayeeSuggestor()

            val mccs = listOf(4829)
            statementItems
                .filter { it.mcc !in mccs }
                .filter { settings.mappings.getMccCategoryOverride(it.mcc) == null }
                .filter {
                    suggestor(it.description, payeeNames).isEmpty()
                }
                .forEach {
                    val suggestions = suggestor(it.description, payeeNames)

                    println(
                        "Desc: ${it.description}, mcc: ${it.mcc}-${MCC.mapRussian[it.mcc]}\n\tSuggested payees: ${
                        suggestions.joinToString(", ")
                        }\n"
                    )
                }
        }
    }

    @Test
    @Disabled
    fun `Send statement message`() {
        val settings = Settings.loadDefault()
        val telegram = TelegramApi(
            settings.telegramBotUsername,
            settings.telegramBotToken,
            settings.mappings.getTelegramChatIds()
        )
        val description = "vasa"
        val monoAccount = "ps7BhBZPtgiR_36jfhYXlg"
        val handler = TelegramHandler(telegram, settings.mappings)
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
        every { transaction.id } returns UUID.randomUUID().toString()

        runBlocking {
            handler.sendStatementMessage(
                Event.Telegram.SendStatementMessage(monoResponse, transaction)
            )
        }
    }
}
