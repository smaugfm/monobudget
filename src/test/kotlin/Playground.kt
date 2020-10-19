import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.util.PayeeSuggestor
import com.github.smaugfm.ynab.YnabApi
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.time.days

class Playground {
    @Test
    fun `Get last 29 days Mono transactions`() {
        runBlocking {
            val settings = Settings.loadDefault()
            val mono = MonoApi(settings.monoTokens.first())
            val ynab = YnabApi(settings.ynabToken, settings.ynabBudgetId)
            val statementItems = mono.fetchStatementItems(
                settings.mappings.getMonoAccounts().find { it.startsWith("p") }!!,
                Clock.System.now() - 59.days,
                Clock.System.now() - 29.days,
            )

            val payees = ynab.getPayees()
            val payeeNames = payees.map { it.name }
            val suggestor = PayeeSuggestor()

            statementItems.forEach {
                val suggestions = suggestor(it.description, payeeNames)

                println("\tDesc: ${it.description}, Suggested payees: ${suggestions.joinToString(", ")}\n")
            }
        }
    }
}
