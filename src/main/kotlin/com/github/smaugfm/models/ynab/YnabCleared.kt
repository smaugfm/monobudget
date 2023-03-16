@file:Suppress("EnumEntryName")

package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
enum class YnabCleared {
    cleared,
    uncleared,
    reconciled
}
