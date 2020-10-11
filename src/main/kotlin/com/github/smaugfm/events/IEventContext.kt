package com.github.smaugfm.events

import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.apis.YnabApi

interface IEventContext {
    fun resolveYnabAccount(monoAccountId: String): String?
    fun resolveTelegramAccount(monoAccountId: String): Long?

    suspend fun dispatch(event: Event): Unit
}