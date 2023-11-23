package io.github.smaugfm.monobudget.common.model.settings

import io.github.smaugfm.monobudget.common.model.serializer.RegexAsStringSerializer
import kotlinx.serialization.Serializable

@Serializable
data class OtherBanksTransferSettings(
    @Serializable(RegexAsStringSerializer::class)
    val descriptionRegex: Regex,
    val transferDescription: String,
    val transferAccountId: String,
    val mcc: Int,
)
