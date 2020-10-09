package com.github.smaugfm.processing

import com.github.kotlintelegrambot.Bot
import com.github.smaugfm.mono.model.MonoWebHookResponseData
import com.github.smaugfm.ynab.YnabApi

class TelegramAction

class EventsProcessor(
    override val ynab: YnabApi,
    val filters: List<EventsFilter>,
) : IEventProcessingContext {
    suspend fun onNewEvent(event: Event) {
        for (filter in filters) {
            val stop = filter(event, this)
            if (stop)
                break
        }
    }
}


typealias EventsFilter = suspend (event: Event, context: IEventProcessingContext) -> Boolean
