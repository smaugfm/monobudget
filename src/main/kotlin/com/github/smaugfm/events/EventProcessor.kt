package com.github.smaugfm.events

import io.michaelrocks.bimap.BiMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors

class EventProcessor(
    private val monoAcc2Ynab: BiMap<String, String>,
    private val monoAcc2Telegram: Map<String, Long>,
    private val handleCreators: List<IEventHandlerCreator>,
) : IEventContext {
    private val handlers = handleCreators.map { it.create(this::dispatch) }
    private val singleThreaded = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val eventsQueue: Queue<Event> = ArrayDeque()
    private var isDispatching: Boolean = false

    override fun resolveYnabAccount(monoAccountId: String) = monoAcc2Ynab[monoAccountId]
    override fun resolveTelegramAccount(monoAccountId: String) = monoAcc2Telegram[monoAccountId]

    override suspend fun dispatch(event: Event) {
        withContext(singleThreaded) {
            eventsQueue.offer(event)
            if (!isDispatching) {
                isDispatching = true
                try {
                    while (eventsQueue.isNotEmpty()) {
                        val current = eventsQueue.poll()

                        for (handler in handlers) {
                            val handled = handler(current)
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
}
