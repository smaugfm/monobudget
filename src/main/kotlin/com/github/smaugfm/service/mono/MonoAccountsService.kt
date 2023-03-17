package com.github.smaugfm.service.mono

import com.github.smaugfm.models.settings.Settings
import mu.KotlinLogging
import java.util.Currency

private val logger = KotlinLogging.logger { }

class MonoAccountsService(
    settings: Settings
) {
    private val mappings = settings.mappings
    fun getMonoAccounts() = mappings.monoAccToTelegram.keys

    fun getMonoAccAlias(string: String): String? =
        mappings.monoAccAliases[string].also {
            if (it == null)
                logger.error { "Could not find alias for Monobank account $string" }
        }

    fun getAccountCurrency(monoAccountId: String): Currency? =
        mappings.monoAccToCurrency[monoAccountId]

    fun getTelegramChatIdAccByMono(monoAcc: String) =
        mappings.monoAccToTelegram[monoAcc].also {
            if (it == null)
                logger.error { "Could not find Telegram chatID for Monobank account $monoAcc" }
        }

    fun getYnabAccByMono(monoAcc: String): String? =
        mappings.monoAccToYnab[monoAcc].also {
            if (it == null)
                logger.error { "Could not find YNAB account for Monobank account $monoAcc" }
        }
}
