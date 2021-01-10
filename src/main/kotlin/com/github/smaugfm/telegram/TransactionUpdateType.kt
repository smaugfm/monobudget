package com.github.smaugfm.telegram

import com.elbekD.bot.types.Message
import com.elbekD.bot.types.MessageEntity
import mu.KotlinLogging
import java.util.UUID
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger { }

sealed class TransactionUpdateType {
    abstract val transactionId: String

    data class Uncategorize(override val transactionId: String) : TransactionUpdateType()
    data class Unapprove(override val transactionId: String) : TransactionUpdateType()
    data class Unknown(override val transactionId: String) : TransactionUpdateType()
    data class MakePayee(override val transactionId: String, val payee: String) : TransactionUpdateType()

    companion object {
        fun deserialize(callbackData: String, message: Message): TransactionUpdateType? {
            val cls =
                TransactionUpdateType::class.sealedSubclasses.find { it.simpleName == callbackData }
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

        inline fun <reified T : TransactionUpdateType> serialize(): String {
            return T::class.simpleName!!
        }

        fun KClass<out TransactionUpdateType>.buttonWord(): String {
            return when (this) {
                Uncategorize::class -> "категорию"
                Unapprove::class -> "unapprove"
                Unknown::class -> "невыясненные"
                MakePayee::class -> "payee"
                else -> throw IllegalArgumentException()
            }
        }

        fun KClass<out TransactionUpdateType>.buttonSymbol(): String {
            return when (this) {
                Uncategorize::class -> "❌"
                Unapprove::class -> "🚫"
                Unknown::class -> "➡️"
                MakePayee::class -> "➕"
                else -> throw IllegalArgumentException()
            }
        }

        inline fun <reified T : TransactionUpdateType> buttonText(pressed: Boolean): String =
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
