package io.github.smaugfm.monobudget.common.transaction

import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.model.callback.PressedButtons
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.common.util.formatAmount
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Currency
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

abstract class TransactionMessageFormatter<TTransaction> : KoinComponent {
    private val monoAccountsService: MonoAccountsService by inject()

    suspend fun format(monoResponse: MonoWebhookResponseData, transaction: TTransaction): MessageWithReplyKeyboard {
        val msg = formatHTMLStatementMessage(
            monoAccountsService.getAccountCurrency(monoResponse.account)!!,
            monoResponse.statementItem,
            transaction
        )
        val markup = getReplyKeyboard(transaction)
        val notify = shouldNotify(transaction)

        return MessageWithReplyKeyboard(
            msg,
            markup,
            notify
        )
    }

    fun getReplyKeyboard(transaction: TTransaction): InlineKeyboardMarkup {
        val pressed = getReplyKeyboardPressedButtons(transaction)
        return getReplyKeyboard(transaction, pressed)
    }

    abstract fun shouldNotify(transaction: TTransaction): Boolean

    abstract fun getReplyKeyboardPressedButtons(
        transaction: TTransaction,
        callbackType: TransactionUpdateType? = null
    ): PressedButtons

    protected abstract fun getReplyKeyboard(transaction: TTransaction, pressed: PressedButtons): InlineKeyboardMarkup

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
            id: String,
            idLink: String? = null
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
                builder.append(if (idLink != null) "<a href=\"$idLink\">$id</a>" else "<pre>$id</pre>")

                builder.toString()
            }
        }

        @JvmStatic
        @Suppress("MagicNumber")
        internal fun extractDescriptionFromOldMessage(oldMessage: Message) =
            oldMessage.entities.find { it.type == MessageEntity.Type.BOLD }
                ?.run { oldMessage.text!!.substring(offset, offset + length) }!!

        @JvmStatic
        @Suppress("MagicNumber")
        internal fun extractFromOldMessage(oldMessage: Message): OldMessageEntities {
            val oldText = oldMessage.text!!
            val oldTextLines = oldText.split("\n").filter { it.isNotBlank() }

            val mcc = oldTextLines[2].trim()
            val currencyText = oldTextLines[3].trim()
            val id = oldTextLines[6].trim()

            return OldMessageEntities(
                mcc,
                currencyText,
                id
            )
        }

        data class OldMessageEntities(
            val mcc: String,
            val currency: String,
            val id: String
        )

        @JvmStatic
        protected fun formatAmountWithCurrency(amount: Long, currency: Currency) =
            currency.formatAmount(amount) + currency.currencyCode

        internal fun <T : TransactionUpdateType> callbackData(cls: KClass<out T>) = cls.simpleName!!
    }
}
