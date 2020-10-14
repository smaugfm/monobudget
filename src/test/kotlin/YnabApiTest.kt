import com.github.smaugfm.events.Event
import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.handlers.YnabHandler
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.ynab.YnabApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class YnabApiTest {
    private val settings = Settings.loadDefault()
    private val api = YnabApi(settings.ynabToken, "692ab9be-240a-4847-96c1-80aa21709e9c")
    private val handler = YnabHandler(api, settings.mappings)
    private val accountId = "1355f021-05fc-446b-b2e8-19eb44dd8ede"

    @Test
    fun `Get account transactions`() {
        runBlocking {
            println(api.getAccountTransactions(accountId))
        }
    }

    @Test
    fun `Get single transaction`() {
        runBlocking {
            val id = api.getAccountTransactions(accountId).first().id
            println(api.getTransaction(id))
        }
    }

    @Test
    fun `Update transaction`() {
        runBlocking {
            val id = api.getAccountTransactions(accountId).first().id
            val detail = api.getTransaction(id)
            val save = YnabHandler.ynabTransactionSaveFromDetails(detail)
            println(api.updateTransaction(id, save.copy(memo = "vasa")))
        }
    }

    @Test
    fun `Update transaction in handler`() {
        runBlocking {
            val id = api.getAccountTransactions(accountId).first().id

            handler.updateTransaction(Event.Ynab.UpdateTransaction(id, TelegramHandler.Companion.UpdateType.Unclear))

            handler.updateTransaction(Event.Ynab.UpdateTransaction(id, TelegramHandler.Companion.UpdateType.MarkRed))

            handler.updateTransaction(Event.Ynab.UpdateTransaction(id, TelegramHandler.Companion.UpdateType.Unrecognized))
        }
    }
}