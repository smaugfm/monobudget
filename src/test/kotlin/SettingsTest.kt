import com.github.smaugfm.settings.Settings
import io.michaelrocks.bimap.HashBiMap
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
            webhookURI = URI("http://vasa13.com:8080/vasa12"),
            "vasa1",
            HashBiMap.create(
                mapOf(
                    "vasa2" to "vasa3",
                    "vasa4" to "vasa5"
                )
            ),
            mapOf(
                1223L to listOf("vasa6"),
                12342L to listOf("vasa7")
            )
        )

        settings.save(file)
        val loaded = Settings.load(file)
        assert(settings == loaded)
    }
}