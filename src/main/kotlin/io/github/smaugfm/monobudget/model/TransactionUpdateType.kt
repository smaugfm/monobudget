package io.github.smaugfm.monobudget.model

import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import mu.KotlinLogging
import kotlin.reflect.KClass

private val log = KotlinLogging.logger { }

sealed class TransactionUpdateType {
    abstract val transactionId: String

    data class Uncategorize(override val transactionId: String) : TransactionUpdateType()
    data class Unapprove(override val transactionId: String) : TransactionUpdateType()
    data class MakePayee(override val transactionId: String, val payee: String) : TransactionUpdateType()

    companion object {
        fun deserialize(callbackData: String, message: Message): TransactionUpdateType? {
            val cls =
                TransactionUpdateType::class.sealedSubclasses.find { it.simpleName == callbackData }
            if (cls == null) {
                log.error("Cannot find TransactionActionType. Data: $callbackData")
                return null
            }

            val payee = extractPayee(message) ?: return null
            val transactionId = extractTransactionId(message.text!!)

            return when (cls) {
                Uncategorize::class -> Uncategorize(transactionId)
                Unapprove::class -> Unapprove(transactionId)
                MakePayee::class -> MakePayee(transactionId, payee)
                else -> throw IllegalArgumentException("cls: ${cls.simpleName}")
            }
        }
        fun <T : TransactionUpdateType> serialize(cls: KClass<out T>) = cls.simpleName!!

        inline fun <reified T : TransactionUpdateType> serialize() = serialize(T::class)

        fun KClass<out TransactionUpdateType>.buttonWord(): String {
            return when (this) {
                Uncategorize::class -> "категорію"
                Unapprove::class -> "unapprove"
                MakePayee::class -> "payee"
                else -> throw IllegalArgumentException("Unknown ${TransactionUpdateType::class.simpleName} $this")
            }
        }

        fun KClass<out TransactionUpdateType>.buttonSymbol(): String {
            return when (this) {
                Uncategorize::class -> "❌"
                Unapprove::class -> "🚫"
                MakePayee::class -> "➕"
                else -> throw IllegalArgumentException("Unknown ${TransactionUpdateType::class.simpleName} $this")
            }
        }

        fun <T : TransactionUpdateType> buttonText(cls: KClass<out T>, pressed: Boolean): String = if (pressed) {
            "$pressedChar${cls.buttonWord()}"
        } else {
            "${cls.buttonSymbol()}${cls.buttonWord()}"
        }

        inline fun <reified T : TransactionUpdateType> buttonText(pressed: Boolean): String =
            buttonText(T::class, pressed)

        internal fun extractPayee(message: Message): String? {
            val text = message.text!!
            val payee =
                message.entities.find { it.type == MessageEntity.Type.BOLD }?.run {
                    text.substring(offset, offset + length)
                } ?: return null

            return payee
        }

        internal fun extractTransactionId(text: String): String {
            return text.substring(text.lastIndexOf('\n')).trim()
        }

        const val pressedChar: Char = '✅'
    }
}
