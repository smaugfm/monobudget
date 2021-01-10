package com.github.smaugfm.ynab

import com.github.smaugfm.events.Event
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.telegram.TransactionUpdateType
import com.github.smaugfm.ynab.handlers.UpdateTransactionHandler
import com.github.smaugfm.ynab.handlers.YnabHandler
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayAt
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class YnabApiTest {
    private val settings = Settings.loadDefault()
    private val api = YnabApi(settings.ynabToken, "692ab9be-240a-4847-96c1-80aa21709e9c")

    private val handler = YnabHandler(api, settings.mappings)
    private val accountId = "1355f021-05fc-446b-b2e8-19eb44dd8ede"

    @Test
    fun `Get account transactions`() {
        runBlocking {
            println(api.getAccountTransactions(accountId))
        }
    }

    @Test
    fun `Get single transaction`() {
        runBlocking {
            val id = api.getAccountTransactions(accountId).first().id
            println(api.getTransaction(id))
        }
    }

    @Test
    fun `Update transaction`() {
        runBlocking {
            val id = api.getAccountTransactions(accountId).first().id
            val detail = api.getTransaction(id)
            val save = detail.toSaveTransaction()
            println(api.updateTransaction(id, save.copy(memo = "vasa")))
        }
    }

    @Disabled
    @Test
    fun `Create transaction`() {
        runBlocking {
            // val category = api.getCategories()[5]
            val transaction = YnabSaveTransaction(
                "1355f021-05fc-446b-b2e8-19eb44dd8ede",
                Clock.System.todayAt(TimeZone.currentSystemDefault()),
                10000,
                null,
                "vasa",
                null,
                null,
                YnabCleared.Cleared,
                false,
                null,
                null,
                emptyList()
            )

            val created = api.createTransaction(transaction)
            println(created)
        }
    }

    @Test
    fun `Get categories`() {
        runBlocking {
            api
                .getCategories()
                .map { it.categories }
                .flatten()
                .map { it.id to it.name }
                .forEach {
                    println("${it.first} - ${it.second}")
                }
        }
        println()
    }

    @Test
    fun `Get payees`() {
        runBlocking {
            api.getPayees().map { it.id to it.name }.forEach { println(it) }
        }
        println()
    }

    @Test
    fun `Update transaction in handler`() {
        runBlocking {
            val payee = "vasa"
            val id = api.getAccountTransactions(accountId).first().id
            val updateTransactionHandler = handler.handlers.filterIsInstance<UpdateTransactionHandler>().first()

            updateTransactionHandler.handle(Event.Ynab.UpdateTransaction(TransactionUpdateType.Uncategorize(id)))
            updateTransactionHandler.handle(Event.Ynab.UpdateTransaction(TransactionUpdateType.Unapprove(id)))
            updateTransactionHandler.handle(Event.Ynab.UpdateTransaction(TransactionUpdateType.MakePayee(id, payee)))
        }
    }
}
