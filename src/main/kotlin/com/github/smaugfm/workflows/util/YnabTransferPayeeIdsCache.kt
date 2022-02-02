package com.github.smaugfm.workflows.util

import com.github.smaugfm.apis.YnabApi
import java.util.concurrent.ConcurrentHashMap

class YnabTransferPayeeIdsCache(private val ynab: YnabApi) {
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun get(accountId: String): String =
        cache.getOrPut(accountId) {
            ynab.getAccount(accountId).transfer_payee_id
        }
}
