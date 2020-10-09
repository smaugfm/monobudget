package com.github.smaugfm.events

import com.github.smaugfm.wrappers.TelegramApi
import com.github.smaugfm.wrappers.YnabApi

interface IEventContext {
    fun resolveYnabAccount(monoAccountId: String): String?
    fun resolveMonoAccounts(telegramChatId: Long): List<String>

    val ynab: YnabApi
    val telegram: TelegramApi
    suspend fun dispatch(event: ExternalEvent): Unit
}