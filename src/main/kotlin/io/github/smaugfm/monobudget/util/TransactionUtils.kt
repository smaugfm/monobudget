package io.github.smaugfm.monobudget.util

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertOrUpdateTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction

fun LunchmoneyTransaction.toInsertOrUpdateTransaction(): LunchmoneyInsertOrUpdateTransaction =
    LunchmoneyInsertOrUpdateTransaction(
        date = this.date,
        amount = this.amount,
        categoryId = this.categoryId,
        payee = this.payee,
        currency = this.currency,
        assetId = this.assetId,
        recurringId = this.recurringId,
        notes = this.notes,
        status = this.status,
        externalId = this.externalId,
        tags = this.tags
    )
