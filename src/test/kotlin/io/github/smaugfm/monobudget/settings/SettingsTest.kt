package io.github.smaugfm.monobudget.settings

import assertk.assertThat
import assertk.assertions.isSuccess
import io.github.smaugfm.monobudget.models.Settings
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.readText

class SettingsTest {
    @Test
    fun defaultLoad() {
        assertThat { Settings.load(Paths.get("settings.json").readText()) }.isSuccess()
    }
}
