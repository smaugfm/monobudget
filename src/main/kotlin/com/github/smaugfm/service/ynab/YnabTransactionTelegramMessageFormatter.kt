package com.github.smaugfm.service.ynab

import com.elbekd.bot.types.InlineKeyboardButton
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.ReplyKeyboard
import com.github.smaugfm.models.TransactionUpdateType
import com.github.smaugfm.models.TransactionUpdateType.Companion.buttonText
import com.github.smaugfm.models.TransactionUpdateType.Companion.serialize
import com.github.smaugfm.models.ynab.YnabTransactionDetail
import com.github.smaugfm.service.mono.MonoAccountsService
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import com.github.smaugfm.util.replaceNewLines
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import mu.KotlinLogging
import java.util.Currency
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class YnabTransactionTelegramMessageFormatter(
    private val monoAccountsService: MonoAccountsService,
) {
    fun format(
        monoResponse: MonoWebhookResponseData,
        transaction: YnabTransactionDetail
    ): Triple<String, String, ReplyKeyboard>? {

        val msg = formatHTMLStatementMessage(
            monoAccountsService.getMonoAccAlias(monoResponse.account)!!,
            monoAccountsService.getAccountCurrency(monoResponse.account)!!,
            monoResponse.statementItem,
            transaction
        )
        val markup = formatInlineKeyboard(emptySet())

        val chatId = monoAccountsService.getTelegramChatIdAccByMono(monoResponse.account)
        if (chatId == null) {
            logger.error { "Failed to map Monobank account to telegram chat id. Account: ${monoResponse.account}" }
            return null
        }

        return Triple(
            monoResponse.account,
            msg,
            markup
        )
    }

    companion object {
        internal fun formatInlineKeyboard(
            pressed: Set<KClass<out TransactionUpdateType>>,
        ): InlineKeyboardMarkup {
            return InlineKeyboardMarkup(
                listOf(
                    listOf(
                        button<TransactionUpdateType.Uncategorize>(pressed),
                        button<TransactionUpdateType.Unapprove>(pressed),
                    ),
                    listOf(
                        button<TransactionUpdateType.Unknown>(pressed),
                        button<TransactionUpdateType.MakePayee>(pressed),
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
            id: String,
        ): String {
            logger.info {
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
            transaction: YnabTransactionDetail,
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
                    transaction.id,
                )
            }
        }

        private inline fun <reified T : TransactionUpdateType> button(
            pressed: Set<KClass<out TransactionUpdateType>>
        ) =
            with(T::class) {
                InlineKeyboardButton(
                    buttonText<T>(this in pressed), callbackData = serialize<T>()
                )
            }
    }
}
