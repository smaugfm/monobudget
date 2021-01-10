package com.github.smaugfm.ynab.handlers

import com.github.smaugfm.events.CompositeHandler
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.ynab.YnabApi

class YnabHandler(
    ynab: YnabApi,
    mappings: Mappings,
) : CompositeHandler(
    listOf(
        UpdateTransactionHandler(ynab, mappings),
        CreateTransactionHandler(ynab, mappings),
    ),
)
