package com.github.smaugfm.telegram

import com.elbekD.bot.types.Message
import com.elbekD.bot.types.MessageEntity
import mu.KotlinLogging
import java.util.UUID
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger { }

sealed class TransactionActionType {
    abstract val transactionId: String

    data class Uncategorize(override val transactionId: String) : TransactionActionType()
    data class Unapprove(override val transactionId: String) : TransactionActionType()
    data class Unknown(override val transactionId: String) : TransactionActionType()
    data class MakePayee(override val transactionId: String, val payee: String) : TransactionActionType()

    companion object {
        fun deserialize(callbackData: String, message: Message): TransactionActionType? {
            val cls =
                TransactionActionType::class.sealedSubclasses.find { it.simpleName == callbackData }
            if (cls == null) {
                logger.error("Cannot find TransactionActionType. Data: $callbackData")
                return null
            }

            val (payee, transactionId) =
                extractDescriptionAndTransactionId(message) ?: return null

            return when (cls) {
                Uncategorize::class -> Uncategorize(transactionId)
                Unapprove::class -> Unapprove(transactionId)
                Unknown::class -> Unknown(transactionId)
                MakePayee::class -> MakePayee(transactionId, payee)
                else -> throw IllegalArgumentException("cls: ${cls.simpleName}")
            }
        }

        fun KClass<out TransactionActionType>.serialize(): String {
            return this::class.simpleName!!
        }

        fun KClass<out TransactionActionType>.buttonWord(): String {
            return when (this) {
                Uncategorize::class -> "категорию"
                Unapprove::class -> "unapprove"
                Unknown::class -> "невыясненные"
                MakePayee::class -> "payee"
                else -> throw IllegalArgumentException()
            }
        }

        fun KClass<out TransactionActionType>.buttonSymbol(): String {
            return when (this) {
                Uncategorize::class -> "❌"
                Unapprove::class -> "🚫"
                Unknown::class -> "➡️"
                MakePayee::class -> "➕"
                else -> throw IllegalArgumentException()
            }
        }

        inline fun <reified T : TransactionActionType> buttonText(pressed: Boolean): String =
            if (pressed)
                "$pressedChar${T::class.buttonWord()}"
            else
                "${T::class.buttonSymbol()}${T::class.buttonWord()}"

        fun extractDescriptionAndTransactionId(message: Message): Pair<String, String>? {
            val text = message.text!!
            val id = try {
                UUID.fromString(text.substring(text.length - UUIDwidth, text.length))
            } catch (e: IllegalArgumentException) {
                return null
            }.toString()
            val payee =
                message.entities?.find { it.type == MessageEntity.Types.BOLD.type }?.run {
                    text.substring(offset, offset + length)
                } ?: return null

            return Pair(payee, id)
        }

        const val pressedChar: Char = '✅'
        private const val UUIDwidth = 36
    }
}
