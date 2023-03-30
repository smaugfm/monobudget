package io.github.smaugfm.monobudget.util

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.misc.MCC
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyCategorySuggestionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Paths
import kotlin.io.path.readText

@Disabled
@OptIn(DelicateCoroutinesApi::class)
class Playground {
    val categorySuggestion = LunchmoneyCategorySuggestionService()

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val settings = io.github.smaugfm.monobudget.common.model.Settings.load(Paths.get("settings.yml").readText())
            startKoin {
                modules(
                    module {
                        single { settings.budgetBackend as BudgetBackend.YNAB } bind BudgetBackend::class
                        single { PeriodicFetcherFactory() }
                        single<CoroutineScope> { GlobalScope }
                        single { settings.mcc }
                        single { LunchmoneyApi(settings.budgetBackend.token) }
                    }
                )
            }
        }
    }

    @Test
    @Disabled
    fun test() {
        runBlocking {
            val csv = Csv {
                hasHeaderRecord = true
            }

            println()
            println()
            println()

            val output = csv.decodeFromString<List<CsvMonoItem>>(
                Paths.get("/Users/smaugfm/Downloads/report_26-03-2023_11-30-38.csv").toFile().readText()
            ).map {
                CsvOutputItem(
                    it.time,
                    categorySuggestion.categoryNameByMcc(it.mcc.toInt()) ?: "",
                    it.details,
                    if (it.currency == "UAH") it.amount + it.currency else it.operationAmount + it.currency,
                    "${it.mcc} " + MCC.map[it.mcc.toInt()]?.fullDescription
                )
            }

            Paths.get("/Users/smaugfm/Downloads/output.csv").toFile().writeText(
                csv.encodeToString(output)
            )
        }
    }

    @Serializable
    data class CsvOutputItem(
        @SerialName("Дата i час операції")
        val time: String,

        @SerialName("Запропонована категорія")
        val suggestedCategory: String,

        @SerialName("Деталі операції")
        val details: String,

        @SerialName("Сума операції")
        val amount: String,

        @SerialName("MCC деталі")
        val mcc: String
    )

    @Serializable
    data class CsvMonoItem(
        @SerialName("Дата i час операції")
        val time: String,

        @SerialName("Деталі операції")
        val details: String,

        @SerialName("MCC")
        val mcc: String,

        @SerialName("Сума в валюті картки (UAH)")
        val amount: String,

        @SerialName("Сума в валюті операції")
        val operationAmount: String,

        @SerialName("Валюта")
        val currency: String,

        @SerialName("Курс")
        val exchangeRate: String,

        @SerialName("Сума комісій (UAH)")
        val commissionAmount: String,

        @SerialName("Сума кешбеку (UAH)")
        val cashbackAmount: String,

        @SerialName("Залишок після операції")
        val balance: String
    )
}
