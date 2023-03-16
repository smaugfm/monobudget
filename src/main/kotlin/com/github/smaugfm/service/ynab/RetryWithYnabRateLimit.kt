package com.github.smaugfm.service.ynab

import com.elbekd.bot.model.ChatId
import com.github.smaugfm.util.YnabRateLimitException
import com.github.smaugfm.service.telegram.TelegramHTMLMessageSender
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class RetryWithYnabRateLimit(private val sendMessage: TelegramHTMLMessageSender) {
    private val message = "Сильно багато запитів до YNAB API. " +
        "Я спробую знову через деякий час"

    suspend operator fun invoke(id: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: YnabRateLimitException) {
            sendMessage(
                id,
                this.message,
            )
            retry(block)
        }
    }

    suspend operator fun invoke(chatId: ChatId, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: YnabRateLimitException) {
            sendMessage(
                chatId,
                this.message,
            )
            retry(block)
        }
    }

    private suspend fun retry(block: suspend () -> Unit) {
        coroutineScope {
            launch {
                val waitTime = Duration.between(
                    LocalDateTime.now(),
                    LocalDateTime.now()
                        .withMinute(0)
                        .plusHours(1)
                        .plusMinutes(1)
                )
                delay(waitTime.toMillis())
                block()
            }
        }
    }
}
