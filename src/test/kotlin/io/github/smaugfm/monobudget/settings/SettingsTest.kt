package io.github.smaugfm.monobudget.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import com.uchuhimo.collections.biMapOf
import io.github.smaugfm.monobudget.models.settings.Mappings
import io.github.smaugfm.monobudget.models.settings.Settings
import io.github.smaugfm.monobudget.util.makeJson
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Currency
import kotlin.io.path.readText

class SettingsTest {
    private val json = makeJson()

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
                mapOf(
                    "vasa2" to "vasa2",
                    "vasa4" to "vasa4"
                ),
                biMapOf(
                    "vasa2" to "vasa3",
                    "vasa4" to "vasa5"
                ),
                mapOf("aaa" to Currency.getInstance("UAH")),
                biMapOf(
                    "vasa6" to 12324,
                    "vasa7" to 123242
                ),
                mapOf(
                    12342 to "vasa12",
                    12342 to "vasa13"
                ),
                "vasa14",
                "vasa15"
            )
        )

        File(file.toString()).writeText(json.encodeToString(serializer(), settings))
        val loaded = Settings.load(file)

        assertThat(settings).isEqualTo(loaded)
    }

    @Test
    fun defaultLoad() {
        assertThat { Settings.load(Paths.get("settings.json").readText()) }.isSuccess()
    }
}
