package com.github.smaugfm.events

import com.github.smaugfm.wrappers.TelegramApi
import com.github.smaugfm.wrappers.YnabApi
import io.michaelrocks.bimap.BiMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors

class EventProcessor(
    private val mono2Ynab: BiMap<String, String>,
    private val telegram2Mono: Map<Long, List<String>>,
    private val handlers: List<(ExternalEvent, IEventContext) -> Boolean>,
) : IEventContext {

    private val singleThreaded = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    override lateinit var ynab: YnabApi
    override lateinit var telegram: TelegramApi
    private val eventsQueue: Queue<ExternalEvent> = ArrayDeque()
    private var isDispatching: Boolean = false

    override fun resolveYnabAccount(monoAccountId: String) = mono2Ynab[monoAccountId]
    override fun resolveMonoAccounts(telegramChatId: Long) = telegram2Mono[telegramChatId] ?: emptyList()

    override suspend fun dispatch(event: ExternalEvent) {
        withContext(singleThreaded) {
            eventsQueue.offer(event)
            if (!isDispatching) {
                isDispatching = true
                try {
                    while (eventsQueue.isNotEmpty()) {
                        val current = eventsQueue.poll()

                        for (handler in handlers) {
                            val handled = handler(current, this@EventProcessor)
                            if (handled)
                                break
                        }
                    }
                } finally {
                    isDispatching = false
                }
            }
        }
    }

    fun init(ynabApi: YnabApi, telegramApi: TelegramApi) {
        ynab = ynabApi
        telegram = telegramApi
    }
}
