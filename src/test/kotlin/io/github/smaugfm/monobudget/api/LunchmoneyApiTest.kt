package io.github.smaugfm.monobudget.api

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.model.BudgetBackend
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.makeJson
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.readText

class LunchmoneyApiTest {

    private val settings = Settings.load(Paths.get("settings.json").readText())
    private val api = LunchmoneyApi(
        (settings.budgetBackend as BudgetBackend.Lunchmoney).token
    )
    private val json = makeJson()

    @Test
    @Disabled
    fun dumpAllAccounts() {
        val assets = api
            .getAllAssets()
            .block()

        println(json.encodeToString(assets))
    }

    @Test
    @Disabled
    fun dumpAllCategories() {
        val assets = api.getAllCategories()
            .block()

        println(json.encodeToString(assets))
    }
}
