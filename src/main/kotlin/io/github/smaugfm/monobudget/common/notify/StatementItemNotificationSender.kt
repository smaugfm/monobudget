package io.github.smaugfm.monobudget.common.notify

import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard

interface StatementItemNotificationSender {
    suspend fun notify(
        accountId: BankAccountId,
        newMessage: MessageWithReplyKeyboard,
    )
}
