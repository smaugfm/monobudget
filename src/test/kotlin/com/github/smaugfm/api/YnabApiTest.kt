package com.github.smaugfm.api

import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import io.github.smaugfm.monobank.MonobankPersonalApi
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalSerializationApi::class)
@Suppress("DeferredResultUnused")
class YnabApiTest {
    init {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    }

    private val api = YnabApi(Settings.load(Paths.get("settings.json").readText()))

    @Test
    fun mono() {
        val accountId = ""
        val api = MonobankPersonalApi("")
        val statements = api.getClientStatements(accountId, Clock.System.now().minus(31.days)).block()!!
        val str = statements.joinToString("\n") { item ->
            "${item.description} \t\t\t\t ${item.currencyCode.formatAmount(item.amount)} ${item.currencyCode}\t" +
                " ${item.mcc} ${MCC.map[item.mcc]?.let { "${it.fullDescription} (${it.group.type} ${it.group.description}) " }}" +
                "${item.time.toLocalDateTime(TimeZone.currentSystemDefault())}"
        }
        println(str)
    }

    @Test
    @Disabled
    fun testAllEndpointsDontFail() {
        runBlocking {
            val accountsDeferred = assertDoesNotThrow { api.getAccounts() }
            assertDoesNotThrow { api.getCategories() }
            assertDoesNotThrow { api.getPayees() }
            accountsDeferred.let { accounts ->
                if (accounts.isNotEmpty()) {
                    assertDoesNotThrow { api.getAccount(accounts.first().id) }
                    assertDoesNotThrow { api.getAccountTransactions(accounts.first().id) }
                }
            }
        }
    }
}
