import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.util.HashBiMap
import org.junit.jupiter.api.Test
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
            "vasa1",
            Mappings(
                HashBiMap.of(
                    "vasa2" to "vasa3",
                    "vasa4" to "vasa5"
                ),
                mapOf(
                    "vasa6" to 12324,
                    "vasa7" to 123242,
                ),
                mapOf(
                    12342 to "vasa12",
                    12342 to "vasa13",
                ),
                mapOf("12341" to "vasa"),
                "vasa14",
                "vasa15"
            )
        )

        settings.save(file)
        val loaded = Settings.load(file)

        assertThat(settings).isEqualTo(loaded)
    }

    @Test
    fun defaultLoad() {
        assertThat { Settings.loadDefault() }.isSuccess()
    }
}
