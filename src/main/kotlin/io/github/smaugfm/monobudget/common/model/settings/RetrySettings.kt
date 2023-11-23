package io.github.smaugfm.monobudget.common.model.settings

import io.github.smaugfm.monobudget.common.model.serializer.SpringLikeDurationDeserializer
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class RetrySettings(
    @Serializable(SpringLikeDurationDeserializer::class)
    val interval: Duration = 15.minutes,
)
