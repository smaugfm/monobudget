package io.github.smaugfm.monobudget.settings

import assertk.assertThat
import assertk.assertions.isSuccess
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.readText

class SettingsTest {
    @Test
    fun defaultLoad() {
        assertThat {
            io.github.smaugfm.monobudget.common.model.Settings.load(
                Paths.get("settings.yml").readText()
            )
        }.isSuccess()
    }
}
