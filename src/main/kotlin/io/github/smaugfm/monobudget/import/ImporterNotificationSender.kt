package io.github.smaugfm.monobudget.import

import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.common.notify.StatementItemNotificationSender

object ImporterNotificationSender : StatementItemNotificationSender {
    override suspend fun notify(
        accountId: BankAccountId,
        newMessage: MessageWithReplyKeyboard,
    ) {
        // no-op
    }
}
