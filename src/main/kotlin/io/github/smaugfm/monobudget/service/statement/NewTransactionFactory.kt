package io.github.smaugfm.monobudget.service.statement

import io.github.smaugfm.monobank.model.MonoWebhookResponseData

abstract class NewTransactionFactory<TNewTransaction>() {
    abstract suspend fun create(response: MonoWebhookResponseData): TNewTransaction
}
