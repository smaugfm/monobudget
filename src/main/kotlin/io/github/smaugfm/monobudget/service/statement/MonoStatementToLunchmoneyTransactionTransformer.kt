package io.github.smaugfm.monobudget.service.statement

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertOrUpdateTransaction
import io.github.smaugfm.monobank.model.MonoWebhookResponseData

class MonoStatementToLunchmoneyTransactionTransformer {

    fun transform(response: MonoWebhookResponseData): LunchmoneyInsertOrUpdateTransaction {
        TODO(response.toString())
    }
}
