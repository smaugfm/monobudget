package com.github.smaugfm.workflows

import com.github.smaugfm.models.MonoAccountId
import com.github.smaugfm.util.YnabRateLimitException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class RetryWithRateLimit(private val sendMessage: SendHTMLMessageToTelegram) {
    private val message = "Слишком много запросов на YNAB API. " +
        "Я попробую снова через некоторое время."

    suspend operator fun invoke(id: MonoAccountId, block: suspend () -> Unit) {
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

    suspend operator fun invoke(chatId: Long, block: suspend () -> Unit) {
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
