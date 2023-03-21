package io.github.smaugfm.monobudget.service.formatter

import com.elbekd.bot.types.InlineKeyboardButton
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.TransactionUpdateType
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.util.formatAmount
import mu.KotlinLogging
import java.util.Currency
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

sealed class TransactionMessageFormatter<T> {
    abstract suspend fun format(monoResponse: MonoWebhookResponseData, transaction: T): MessageWithReplyKeyboard

    companion object {

        @Suppress("LongParameterList")
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
        internal fun formatInlineKeyboard(pressed: Set<KClass<out TransactionUpdateType>>): InlineKeyboardMarkup {
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
