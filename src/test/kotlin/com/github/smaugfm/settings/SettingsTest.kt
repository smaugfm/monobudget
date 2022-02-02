package com.github.smaugfm.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.models.settings.Settings
import com.uchuhimo.collections.biMapOf
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.Currency

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
                biMapOf(
                    "vasa2" to "vasa3",
                    "vasa4" to "vasa5"
                ),
                mapOf("aaa" to Currency.getInstance("UAH")),
                mapOf(
                    "vasa6" to 12324,
                    "vasa7" to 123242,
                ),
                mapOf(
                    12342 to "vasa12",
                    12342 to "vasa13",
                ),
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
