package io.github.smaugfm.monobudget.service.formatter

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard

object LunchmoneyTransactionMessageFormatter : TransactionMessageFormatter<LunchmoneyTransaction>() {
    override suspend fun format(
        monoResponse: MonoWebhookResponseData,
        transaction: LunchmoneyTransaction
    ): MessageWithReplyKeyboard? {
        TODO("Not yet implemented")
    }
}
