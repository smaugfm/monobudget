package io.github.smaugfm.monobudget.common.misc

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.settings.MonoAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.util.injectAll
import io.github.smaugfm.monobudget.common.util.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyCategoryService
import io.github.smaugfm.monobudget.mono.MonoApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.nio.file.Paths
import kotlin.io.path.readText

@OptIn(DelicateCoroutinesApi::class)
class Playground : KoinTest {
    private val apis: List<MonoApi> by injectAll()

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val settings = Settings.load(Paths.get("settings.yml").readText())
            startKoin {
                modules(
                    module {
                        single { settings.budgetBackend as BudgetBackend.YNAB } bind BudgetBackend::class
                        single { PeriodicFetcherFactory(get()) }
                        single<CoroutineScope> { GlobalScope }
                        single { settings.mcc }
                        single { LunchmoneyApi(settings.budgetBackend.token) }
                        single { LunchmoneyCategoryService(get(), get()) }
                        settings.accounts.settings.filterIsInstance<MonoAccountSettings>()
                            .forEach { s ->
                                single(StringQualifier(s.alias)) {
                                    MonoApi(
                                        s.token,
                                        s.accountId,
                                        s.alias,
                                    )
                                }
                            }
                    },
                )
            }
        }
    }

    @Test
    @Disabled
    fun vasa() {
        println(apis)
    }
}
