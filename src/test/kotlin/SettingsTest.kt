import com.github.smaugfm.settings.Settings
import com.github.smaugfm.util.HashBiMap
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Files

class SettingsTest {
    @Test
    fun testSaveLoad() {
        val file = Files.createTempFile("settings-ynab-bot", ".json")
        val settings = Settings(
            "vasa8",
            listOf("vasa9", "vasa10"),
            "vasa11",
            "vasa14",
            webhookURI = URI("http://vasa13.com:8080/vasa12"),
            "vasa1",
            HashBiMap.of(
                "vasa2" to "vasa3",
                "vasa4" to "vasa5"
            ),
            mapOf(
                "vasa6" to 12324L,
                "vasa7" to 123242L,
            ),
            mapOf(
                12342 to "vasa12",
                12342 to "vasa13",
            )
        )

        settings.save(file)
        val loaded = Settings.load(file)
        assert(loaded == settings)
    }
}