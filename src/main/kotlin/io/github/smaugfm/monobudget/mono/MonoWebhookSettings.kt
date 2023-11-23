package io.github.smaugfm.monobudget.mono

import java.net.URI

data class MonoWebhookSettings(
    val setWebhook: Boolean,
    val monoWebhookUrl: URI,
    val webhookPort: Int,
)
