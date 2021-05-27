package com.github.smaugfm.telegram.handlers

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.MessageEntity
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.IEvent
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.telegram.TransactionUpdateType
import com.github.smaugfm.telegram.TransactionUpdateType.Companion.serialize
import com.github.smaugfm.ynab.YnabTransactionDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.days

class TelegramHandlerTest {
    val chatId = 12322
    val api = mockk<TelegramApi>()
    val mappings = mockk<Mappings>()
    val sendStatementMessageHandler = SendStatementMessageHandler(mappings)
    val callbackQueryHandler = CallbackQueryHandler(api, mappings)

    private fun getMonoResponseAndTransaction(
        description: String,
        payee: String,
        monoAccount: String,
        id: String,
    ): Pair<MonoWebHookResponseData, YnabTransactionDetail> {
        every { mappings.getTelegramChatIds() } returns setOf(chatId)

        val statementItem = mockk<MonoStatementItem>()
        every { statementItem.time } returns Clock.System.now() - Duration.days(
            2
        )
        every { statementItem.amount } returns -11500
        every { statementItem.operationAmount } returns -11500
        every { statementItem.mcc } returns 5722
        every { statementItem.description } returns description
        every { statementItem.currencyCode } returns Currency.getInstance("UAH")

        val monoResponse = mockk<MonoWebHookResponseData>()
        every { monoResponse.account } returns monoAccount
        every { monoResponse.statementItem } returns statementItem

        val transaction = mockk<YnabTransactionDetail>()
        every { transaction.payee_name } returns payee
        every { transaction.category_name } returns "Продукты/быт"
        every { transaction.id } returns id

        return Pair(monoResponse, transaction)
    }

    @Test
    @Disabled
    fun `Send statement message`() {
        val monoAccount = "vasility"
        val payee = "Rozetka"
        coEvery { api.sendMessage(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { mappings.getTelegramChatIds() } returns setOf(chatId)
        every { mappings.getTelegramChatIdAccByMono(any()) } returns chatId
        every { mappings.getAccountCurrency(any()) } returns Currency.getInstance("UAH")

        val dispatcher = mockk<IEventDispatcher>()
        val (monoResponse, transaction) =
            getMonoResponseAndTransaction(payee, payee, monoAccount, UUID.randomUUID().toString())

        runBlocking {
            sendStatementMessageHandler.handle(
                dispatcher,
                Event.Telegram.SendStatementMessage(monoResponse, transaction)
            )
        }
        val message = formatHTMLStatementMessage(Currency.getInstance("UAH"), monoResponse.statementItem, transaction)

        coVerify {
            mappings.getTelegramChatIdAccByMono(monoAccount)
            api.sendMessage(
                chatId,
                message,
                "HTML",
                null,
                null,
                null,
                InlineKeyboardMarkup(
                    listOf(
                        listOf(
                            InlineKeyboardButton(
                                "❌категорию",
                                callback_data = serialize<TransactionUpdateType.Uncategorize>()
                            ),
                            InlineKeyboardButton(
                                "\uD83D\uDEABunapprove",
                                callback_data = serialize<TransactionUpdateType.Unapprove>()
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                "➡️невыясненные",
                                callback_data = serialize<TransactionUpdateType.Unknown>()
                            ),
                            InlineKeyboardButton(
                                "➕payee",
                                callback_data = serialize<TransactionUpdateType.MakePayee>()
                            ),
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `Test extractPayeeAndTransactionIdFromMessage`() {
        val payee = "Галинка Савченко"
        val id = UUID.randomUUID().toString()
        val (monoResponse, transaction) =
            getMonoResponseAndTransaction(payee, payee, "vasa", id)

        val messageText = formatHTMLStatementMessage(
            Currency.getInstance("UAH"),
            monoResponse.statementItem,
            transaction
        )
        val adjustedMessage = stripHTMLTagsFromMessage(messageText)
        val message = mockk<Message> {
            every { text } returns adjustedMessage
            every { entities } returns listOf(
                MessageEntity(
                    "bold",
                    adjustedMessage.indexOf(payee),
                    payee.length,
                    null,
                    null,
                    null
                )
            )
        }
        val (extractedPayee, extractedId) = TransactionUpdateType.extractDescriptionAndTransactionId(
            message
        )!!

        assertThat(extractedPayee).isEqualTo(payee)
        assertThat(extractedId).isEqualTo(id)
    }

    @Test
    @Suppress("LongMethod")
    fun `Test callbackQueryHandler`() {
        val description = "Галинка Савченко"
        val transactionId = UUID.randomUUID().toString()

        val (monoResponse, transaction) =
            getMonoResponseAndTransaction(description, "", "vasa", transactionId)

        coEvery { api.editMessage(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { api.answerCallbackQuery(any()) } returns Unit

        val updatedTransaction = mockk<YnabTransactionDetail> {
            every { payee_name } returns description
            every { category_name } returns transaction.category_name
            every { id } returns transactionId
        }

        val messageText = stripHTMLTagsFromMessage(
            formatHTMLStatementMessage(Currency.getInstance("UAH"), monoResponse.statementItem, transaction)
        )
        val keyboard = formatInlineKeyboard(emptySet())

        val messageId = 123413
        val messageMock = mockk<Message>() {
            every { text } returns messageText
            every { chat.id } returns chatId.toLong()
            every { message_id } returns messageId
            every { entities } returns listOf(
                MessageEntity(
                    "bold",
                    messageText.indexOf(description),
                    description.length,
                    null,
                    null,
                    null
                )
            )
            every { reply_markup } returns keyboard
        }

        val dispatcher = mockk<IEventDispatcher>()
        coEvery { dispatcher.invoke<Any, IEvent<Any>>(any()) } returns updatedTransaction

        val callbackQueryMock = mockk<CallbackQuery>() {
            every { id } returns "vasa"
            every { data } returns TransactionUpdateType.MakePayee::class.simpleName!!
            every { message } returns messageMock
            every { from.id } returns chatId
        }
        val event = mockk<Event.Telegram.CallbackQueryReceived>() {
            every { callbackQuery } returns callbackQueryMock
        }

        runBlocking {
            callbackQueryHandler.handle(dispatcher, event)
        }

        val updatedMessageText =
            formatHTMLStatementMessage(Currency.getInstance("UAH"), monoResponse.statementItem, updatedTransaction)
        val updatedMarkup = formatInlineKeyboard(setOf(TransactionUpdateType.MakePayee::class))

        coVerify {
            dispatcher.invoke(Event.Ynab.UpdateTransaction(TransactionUpdateType.MakePayee(transactionId, description)))
            api.editMessage(
                chatId.toLong(),
                messageId,
                text = updatedMessageText,
                parseMode = "HTML",
                markup = updatedMarkup
            )
        }
    }
}
