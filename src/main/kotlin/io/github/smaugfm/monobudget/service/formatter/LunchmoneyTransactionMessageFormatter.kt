package io.github.smaugfm.monobudget.service.formatter

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard

class LunchmoneyTransactionMessageFormatter<T> : TransactionMessageFormatter<T>() {
    override fun format(monoResponse: MonoWebhookResponseData, transaction: T): MessageWithReplyKeyboard? {
        TODO("Not yet implemented")
    }
}
