package io.github.smaugfm.monobudget.api

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.request.asset.LunchmoneyGetAllAssetsRequest
import io.github.smaugfm.monobudget.models.BudgetBackend
import io.github.smaugfm.monobudget.models.Settings
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.readText

class LunchmoneyApiTest {

    private val settings = Settings.load(Paths.get("settings.json").readText())
    private val api = LunchmoneyApi(
        (settings.budgetBackend as BudgetBackend.Lunchmoney).token
    )

    @Test
    @Disabled
    fun test() {
        val assets = api.execute(LunchmoneyGetAllAssetsRequest())
            .block()!!.assets
        println(assets)
    }
}
