package com.github.smaugfm.telegram

import java.util.logging.Logger
import kotlin.reflect.full.primaryConstructor

sealed class TransactionActionType {
    abstract val transactionId: String

    fun serialize(): String {
        return when (this) {
            is Uncategorize, is Unpayee, is Unapprove, is Unknown -> {
                "${this::class.simpleName}   ${this.transactionId}"
            }
            is MakePayee -> "${this::class.simpleName}   ${this.transactionId}   (${this.payeeName})"
        }
    }

    data class Uncategorize(override val transactionId: String) : TransactionActionType()
    data class Unpayee(override val transactionId: String) : TransactionActionType()
    data class Unapprove(override val transactionId: String) : TransactionActionType()
    data class Unknown(override val transactionId: String) : TransactionActionType()

    data class MakePayee(override val transactionId: String, val payeeName: String) : TransactionActionType()

    companion object {
        private val logger = Logger.getLogger(TransactionActionType::class.qualifiedName.toString())
        private val callbackDataPattern = Regex("(\\S+)   (\\S+)(?:   \\((\\w[\\s\\w]+)\\))?")

        private fun <T : Any?> severe(msg: String): T? {
            logger.severe(msg)
            return null
        }

        fun deserialize(data: String): TransactionActionType? {
            val matchResult = callbackDataPattern.matchEntire(data)
                ?: return severe("Callback query callback_data does not match pattern. $data")

            val typeStr = matchResult.groupValues[1]
            val transactionId = matchResult.groupValues[2]

            return try {
                val type =
                    TransactionActionType::class.sealedSubclasses.find { it.simpleName == typeStr }!!
                if (type == MakePayee::class) {
                    val payeeName = matchResult.groupValues[3]
                    MakePayee(transactionId, payeeName)
                } else {
                    type.primaryConstructor!!.call(transactionId)
                }
            } catch (e: Throwable) {
                severe("Callback query callback_data does not contain known CallbackType. $data")
            }
        }
    }
}
