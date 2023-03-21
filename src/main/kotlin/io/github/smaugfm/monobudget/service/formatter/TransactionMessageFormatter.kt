package io.github.smaugfm.monobudget.service.formatter

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard

sealed class TransactionMessageFormatter<T> {

    abstract suspend fun format(monoResponse: MonoWebhookResponseData, transaction: T): MessageWithReplyKeyboard
}
