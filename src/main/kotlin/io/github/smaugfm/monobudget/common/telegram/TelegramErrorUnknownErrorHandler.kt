package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.exception.BudgetBackendError
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import org.koin.core.annotation.Single

@Single
class TelegramErrorUnknownErrorHandler(
    private val monoSettings: MultipleAccountSettings,
    private val bankAccounts: BankAccountService,
    private val telegramApi: TelegramApi
) {
    suspend fun onUnknownError() {
        monoSettings.telegramChatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = TelegramApi.UNKNOWN_ERROR_MSG
                )
            }
    }

    suspend fun onBudgetBackendError(budgetBackendError: BudgetBackendError) {
        monoSettings.telegramChatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = budgetBackendError.userMessage
                )
            }
    }

    suspend fun onRetry(statementItem: StatementItem) {
        val chatId = bankAccounts.getTelegramChatIdByAccountId(statementItem.accountId)!!
        telegramApi.sendMessage(
            chatId = ChatId.IntegerId(chatId),
            text = RETRY_MSG
        )
    }

    companion object {
        const val RETRY_MSG =
            "Виникла невідома помилка при створенні транзакції. " +
                "Я спробую ще раз трішки пізніше..."
    }
}
