import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.settings.Settings
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
            val statementItems = mono.fetchStatementItems(
                settings.mappings.getMonoAccounts().find { it.startsWith("p") }!!,
                Clock.System.now() - 29.days
            )

            statementItems.forEach { println(it) }
        }
    }
}
