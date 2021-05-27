package com.github.smaugfm.ynab.handlers

import com.github.smaugfm.mono.MonoWebHookResponseData

object UniqueWebhookResponses {
    private val cache = mutableSetOf<String>()

    @Synchronized
    fun isUnique(response: MonoWebHookResponseData): Boolean =
        if (cache.contains(response.statementItem.id)) {
            false
        } else {
            cache.add(response.statementItem.id)
            true
        }
}
