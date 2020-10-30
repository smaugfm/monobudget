package com.github.smaugfm.telegram

import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

sealed class TransactionActionType {
    fun serialize() = this::class.simpleName!!

    object Uncategorize : TransactionActionType()
    object Unapprove : TransactionActionType()
    object Unknown : TransactionActionType()
    object MakePayee : TransactionActionType()

    companion object {
        fun deserialize(data: String): TransactionActionType? {
            val cls =
                TransactionActionType::class.sealedSubclasses.find { it.simpleName == data }
            if (cls == null) {
                logger.error("Cannot find TransactionActionType. Data: $data")
                return null
            }

            return when (cls.simpleName) {
                Uncategorize::class.simpleName -> Uncategorize
                Unapprove::class.simpleName -> Unapprove
                Unknown::class.simpleName -> Unknown
                MakePayee::class.simpleName -> MakePayee
                else -> throw IllegalArgumentException("cls.simpleName: ${cls.simpleName}")
            }
        }
    }
}
