package io.github.smaugfm.monobudget.common.transaction

import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.model.callback.PressedButtons
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Currency
import kotlin.math.roundToLong
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

abstract class TransactionMessageFormatter<TTransaction> : KoinComponent {
    private val bankAccounts: BankAccountService by inject()

    suspend fun format(statement: StatementItem, transaction: TTransaction): MessageWithReplyKeyboard {
        val msg = formatHTMLStatementMessage(
            bankAccounts.getAccountCurrency(statement.accountId)!!,
            statement,
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

    protected abstract fun getReplyKeyboard(
        transaction: TTransaction,
        pressed: PressedButtons
    ): InlineKeyboardMarkup

    protected abstract suspend fun formatHTMLStatementMessage(
        accountCurrency: Currency,
        statementItem: StatementItem,
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
            category: CategoryService.BudgetedCategory?,
            payee: String,
            id: String,
            idLink: String? = null
        ): String {
            log.info {
                "Formatting message:" +
                    "\n\tdescription: $description" +
                    "\n\tmcc: $mcc" +
                    "\n\tamount: $amount" +
                    "\n\tcategory: $category" +
                    "\n\tpayee: $payee" +
                    "\n\tid: $id"
            }
            val builder = StringBuilder("Нова транзакція Monobank додана в $budgetBackend\n")
            return with(Unit) {
                builder.append("\uD83D\uDCB3 <b>$description</b>\n")
                builder.append("      $mcc\n")
                builder.append("      <u>$amount</u>\n")
                builder.append("      <code>Category: ${category?.categoryName ?: ""}</code>\n")
                builder.append("      <code>Payee:    $payee</code>\n")

                val budget = category?.budget
                if (budget != null) {
                    builder.append("\n")
                    formatBudget(budget, builder)
                }
                builder.append("\n\n")
                builder.append(if (idLink != null) "<a href=\"$idLink\">$id</a>" else "<pre>$id</pre>")

                builder.toString()
            }
        }

        @Suppress("MagicNumber")
        fun formatBudget(
            budget: CategoryService.BudgetedCategory.CategoryBudget,
            builder: StringBuilder
        ) {
            val left = budget.left.formatShort()
            val budgeted = budget.budgetedThisMonth.formatShort()
            val percent =
                "${(budget.left.value.toDouble() * 100 / budget.budgetedThisMonth.value).roundToLong()}%"
            val alert = if (budget.left.value < 0) " ⚠️" else ""

            builder.append("Залишок$alert: <code>$left із $budgeted</code> (<b>$percent</b>)")
        }

        @JvmStatic
        @Suppress("MagicNumber")
        fun extractFromOldMessage(message: Message): OldMessageEntities {
            val text = message.text!!
            val textLines = text.split("\n").filter { it.isNotBlank() }
            val description = message.entities.find { it.type == MessageEntity.Type.BOLD }
                ?.run { text.substring(offset, offset + length) }!!

            val mcc = textLines[2].trim()
            val currencyText = textLines[3].trim()

            return OldMessageEntities(
                description,
                mcc,
                currencyText
            )
        }

        fun extractTransactionId(message: Message): String = message.text!!.let {
            it.substring(it.lastIndexOf('\n')).trim()
        }

        fun extractPayee(message: Message): String? {
            val text = message.text!!
            val payee =
                message.entities.find { it.type == MessageEntity.Type.BOLD }?.run {
                    text.substring(offset, offset + length)
                } ?: return null

            return payee
        }

        data class OldMessageEntities(
            val description: String,
            val mcc: String,
            val currency: String
        )

        data class FormattedBudget(
            val left: String,
            val budgeted: String
        )

        internal fun <T : TransactionUpdateType> callbackData(cls: KClass<out T>) = cls.simpleName!!
    }
}
