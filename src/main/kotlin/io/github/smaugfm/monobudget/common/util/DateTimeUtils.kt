package io.github.smaugfm.monobudget.common.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Instant.toLocalDateTime() = this.toLocalDateTime(TimeZone.currentSystemDefault())
