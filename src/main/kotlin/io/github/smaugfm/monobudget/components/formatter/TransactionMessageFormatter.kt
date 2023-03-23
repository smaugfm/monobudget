package io.github.smaugfm.monobudget.components.formatter

import com.elbekd.bot.types.InlineKeyboardButton
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.model.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.util.formatAmount
import mu.KotlinLogging
import java.util.Currency
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

sealed class TransactionMessageFormatter<TTransaction>(
    private val monoAccountsService: MonoAccountsService,
) {
    suspend fun format(
        monoResponse: MonoWebhookResponseData,
        transaction: TTransaction
    ): MessageWithReplyKeyboard {
        val msg = formatHTMLStatementMessage(
            monoAccountsService.getAccountCurrency(monoResponse.account)!!,
            monoResponse.statementItem,
            transaction
        )
        val markup = formatInlineKeyboard()

        return MessageWithReplyKeyboard(
            msg,
            markup
        )
    }

    protected abstract suspend fun formatHTMLStatementMessage(
        accountCurrency: Currency,
        monoStatementItem: MonoStatementItem,
        transaction: TTransaction
    ): String

    companion object {

        @Suppress("LongParameterList")
        @JvmStatic
        internal fun formatHTMLStatementMessage(
            budgetBackend: String,
            description: String,
            mcc: String,
            amount: String,
            category: String,
            payee: String,
            id: String
        ): String {
            log.info {
                "Formatting message:\n" +
                    "\t$description" +
                    "\t$mcc" +
                    "\t$amount" +
                    "\t$category" +
                    "\t$payee" +
                    "\t$id"
            }
            val builder = StringBuilder("Нова транзакція Monobank додана в $budgetBackend\n")
            return with(Unit) {
                builder.append("\uD83D\uDCB3 <b>$description</b>\n")
                builder.append("      $mcc\n")
                builder.append("      <u>$amount</u>\n")
                builder.append("      <code>Category: $category</code>\n")
                builder.append("      <code>Payee:    $payee</code>\n")
                builder.append("\n\n")
                builder.append("<pre>$id</pre>")

                builder.toString()
            }
        }

        @JvmStatic
        internal fun extractFromOldMessage(oldMessage: Message): OldMessageEntities {
            val oldText = oldMessage.text!!
            val oldTextLines = oldText.split("\n").filter { it.isNotBlank() }
            val description = oldMessage.entities.find { it.type == MessageEntity.Type.BOLD }
                ?.run { oldMessage.text!!.substring(offset, offset + length) }!!

            val mcc = oldTextLines[2].trim()
            val currencyText = oldTextLines[3].trim()
            val id = oldTextLines[6].trim()

            return OldMessageEntities(
                description,
                mcc,
                currencyText,
                id
            )
        }

        data class OldMessageEntities(
            val description: String,
            val mcc: String,
            val currency: String,
            val id: String
        )

        @JvmStatic
        internal fun formatInlineKeyboard(pressed: Set<KClass<out TransactionUpdateType>> = emptySet()): InlineKeyboardMarkup {
            return InlineKeyboardMarkup(
                listOf(
                    listOf(
                        button<TransactionUpdateType.Unapprove>(pressed),
                        button<TransactionUpdateType.Uncategorize>(pressed),
                        button<TransactionUpdateType.MakePayee>(pressed)
                    )
                )
            )
        }

        @JvmStatic
        protected fun formatAmountWithCurrency(amount: Long, currency: Currency) =
            currency.formatAmount(amount) + currency.currencyCode

        @JvmStatic
        protected inline fun <reified T : TransactionUpdateType> button(
            pressed: Set<KClass<out TransactionUpdateType>>
        ) = with(T::class) {
            InlineKeyboardButton(
                TransactionUpdateType.buttonText<T>(this in pressed),
                callbackData = TransactionUpdateType.serialize<T>()
            )
        }
    }
}
