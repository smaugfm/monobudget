package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable

/**
 * Опис клієнта та його рахунків. Якщо клієнт не надав права читати його персональні данні, повернеться тільки перелік рахунків.
 */

@Serializable
data class MonoUserInfo(
    val clientId: String,
    val name: String,
    val webHookUrl: String,
    val accounts: List<MonoAccount>
)