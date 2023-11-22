package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.exception.BudgetBackendError
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingEventListener
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import org.koin.core.annotation.Single

@Single
class TelegramErrorHandlerEventListener(
    private val monoSettings: MultipleAccountSettings,
    private val bankAccounts: BankAccountService,
    private val telegramApi: TelegramApi
) : StatementProcessingEventListener.Error {
    override suspend fun handleProcessingError(ctx: StatementProcessingContext, e: Throwable) {
        if (e is BudgetBackendError) {
            onBudgetBackendError(e)
        } else {
            onUnknownError()
        }
    }

    private suspend fun onUnknownError() {
        monoSettings.telegramChatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = TelegramApi.UNKNOWN_ERROR_MSG
                )
            }
    }

    private suspend fun onBudgetBackendError(budgetBackendError: BudgetBackendError) {
        val chatIds =
            bankAccounts.getTelegramChatIdByAccountId(budgetBackendError.bankAccountId)
                ?.let(::listOf)
                ?: monoSettings.telegramChatIds
        chatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = budgetBackendError.userMessage
                )
            }
    }
}
