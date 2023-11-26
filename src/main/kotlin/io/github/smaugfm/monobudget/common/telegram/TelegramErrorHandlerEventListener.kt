package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.CallbackQuery
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.exception.BudgetBackendError
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingEventListener
import io.github.smaugfm.monobudget.common.model.callback.CallbackType
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import org.koin.core.annotation.Single

@Single
class TelegramErrorHandlerEventListener(
    private val monoSettings: MultipleAccountSettings,
    private val bankAccounts: BankAccountService,
    private val telegramApi: TelegramApi,
) : StatementProcessingEventListener.Error,
    StatementProcessingEventListener.CallbackError,
    StatementProcessingEventListener.Retry {
    override suspend fun handleStatementError(
        ctx: StatementProcessingContext,
        e: Throwable,
    ): Boolean {
        val chatIds = chatIdsFromContext(ctx)
        if (e is BudgetBackendError) {
            val header =
                "Виникла помилка при створенні транзакції. " +
                    "Будь ласка створи цю транзакцію вручну. "
            onBudgetBackendError(header, chatIds, e)
        } else {
            onUnknownError(chatIds)
        }
        return true
    }

    override suspend fun handleCallbackError(
        query: CallbackQuery,
        callbackType: CallbackType?,
        e: Throwable,
    ) {
        onUnknownError(listOf(query.from.id))
    }

    override suspend fun handleRetry(
        ctx: StatementProcessingContext,
        e: BudgetBackendError,
    ) {
        // Send retry message only on first attempt
        if (ctx.attempt != 0) {
            return
        }

        val chatIds = chatIdsFromContext(ctx)
        val header =
            "Виникла помилка при створенні транзакції. " +
                "Я буду далі пробувати створити цю транзакцію автоматично. "
        onBudgetBackendError(header, chatIds, e)
    }

    private suspend fun onUnknownError(chatIds: List<Long>) {
        chatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = TelegramApi.UNKNOWN_ERROR_MSG,
                )
            }
    }

    private fun chatIdsFromContext(ctx: StatementProcessingContext): List<Long> =
        bankAccounts.getTelegramChatIdByAccountId(ctx.item.accountId)
            ?.let(::listOf)
            ?: monoSettings.telegramChatIds

    private suspend fun onBudgetBackendError(
        header: String,
        chatIds: List<Long>,
        budgetBackendError: BudgetBackendError,
    ) {
        chatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = header + budgetBackendError.userMessage,
                )
            }
    }
}
