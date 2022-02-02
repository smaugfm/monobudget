package com.github.smaugfm.workflows.util

import com.github.smaugfm.models.MonoWebHookResponseData

class UniqueWebhookResponses {
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
