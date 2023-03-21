package io.github.smaugfm.monobudget.service.formatter

import com.elbekd.bot.types.InlineKeyboardButton
import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.YnabTransactionUpdateType
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.formatAmount
import io.github.smaugfm.monobudget.util.replaceNewLines
import mu.KotlinLogging
import java.util.Currency
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

class YnabTransactionMessageFormatter(
    private val monoAccountsService: MonoAccountsService,
) : TransactionMessageFormatter<YnabTransactionDetail>() {
    override suspend fun format(
        monoResponse: MonoWebhookResponseData,
        transaction: YnabTransactionDetail
    ): MessageWithReplyKeyboard? {
        val msg = formatHTMLStatementMessage(
            monoAccountsService.getMonoAccAlias(monoResponse.account)!!,
            monoAccountsService.getAccountCurrency(monoResponse.account)!!,
            monoResponse.statementItem,
            transaction
        )
        val markup = formatInlineKeyboard(emptySet())

        val chatId = monoAccountsService.getTelegramChatIdAccByMono(monoResponse.account)
        if (chatId == null) {
            log.error { "Failed to map Monobank account to telegram chat id. Account: ${monoResponse.account}" }
            return null
        }

        return MessageWithReplyKeyboard(
            msg,
            markup
        )
    }

    companion object {
        internal fun formatInlineKeyboard(pressed: Set<KClass<out YnabTransactionUpdateType>>): InlineKeyboardMarkup {
            return InlineKeyboardMarkup(
                listOf(
                    listOf(
                        button<YnabTransactionUpdateType.Unapprove>(pressed),
                        button<YnabTransactionUpdateType.Uncategorize>(pressed),
                        button<YnabTransactionUpdateType.MakePayee>(pressed)
                    )
                )
            )
        }

        @Suppress("LongParameterList")
        internal fun formatHTMLStatementMessage(
            accountAlias: String?,
            description: String,
            mcc: String,
            amount: String,
            category: String,
            payee: String,
            id: String
        ): String {
            log.info {
                "Formatting message${if (accountAlias != null) " to $accountAlias" else ""}\n" +
                    "\t$description" +
                    "\t$mcc" +
                    "\t$amount" +
                    "\t$category" +
                    "\t$payee" +
                    "\t$id"
            }

            val builder = StringBuilder("Нова транзакція Monobank додана в YNAB\n")
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

        private fun formatAmountWithCurrency(amount: Long, currency: Currency) =
            currency.formatAmount(amount) + currency.currencyCode

        internal fun formatHTMLStatementMessage(
            accountAlias: String,
            accountCurrency: Currency,
            monoStatementItem: MonoStatementItem,
            transaction: YnabTransactionDetail
        ): String {
            with(monoStatementItem) {
                val accountAmount = formatAmountWithCurrency(amount, accountCurrency)
                val operationAmount = formatAmountWithCurrency(this.operationAmount, currencyCode)
                return formatHTMLStatementMessage(
                    accountAlias,
                    (description ?: "").replaceNewLines(),
                    (MCC.map[mcc]?.shortDescription ?: "Невідомий MCC") + " ($mcc)",
                    accountAmount + (if (accountCurrency != currencyCode) " ($operationAmount)" else ""),
                    transaction.categoryName ?: "",
                    transaction.payeeName ?: "",
                    transaction.id
                )
            }
        }

        private inline fun <reified T : YnabTransactionUpdateType> button(
            pressed: Set<KClass<out YnabTransactionUpdateType>>
        ) = with(T::class) {
            InlineKeyboardButton(
                YnabTransactionUpdateType.buttonText<T>(this in pressed),
                callbackData = YnabTransactionUpdateType.serialize<T>()
            )
        }
    }
}
