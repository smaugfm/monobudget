package com.github.smaugfm.workflows

import com.github.smaugfm.models.MonoExportCsvRow
import com.github.smaugfm.models.MonoStatementItem
import com.github.smaugfm.models.MonoWebHookResponseData
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.util.chunked
import com.github.smaugfm.util.makeCsv
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.builtins.ListSerializer
import mu.KotlinLogging
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

class HandleCsv(
    private val createTransaction: CreateTransaction,
    private val sendTransactionCreatedMessage: SendTransactionCreatedMessage,
    private val mappings: Mappings,
) {
    private val csv = makeCsv()

    suspend operator fun invoke(chatId: Long, file: String) {
        logger.info("Starting CSV processing")
        val rows =
            csv.decodeFromString(ListSerializer(MonoExportCsvRow.serializer()), file)
        logger.info("Read ${rows.size} rows. Creating transactions...")

        rows
            .asFlow()
            .chunked(RATE_LIMITING_BUFFER_SIZE)
            .onEach { delay(1.hours) }
            .collect {
                logger.info("Creating ${it.size} transactions.")
                it
                    .map { row ->
                        statementFromRow(chatId, row)
                    }
                    .filterNotNull()
                    .forEach { statement ->
                        val newTransaction = createTransaction(statement) ?: return@forEach
                        sendTransactionCreatedMessage(statement, newTransaction)
                    }
            }
    }

    private fun statementFromRow(chatId: Long, row: MonoExportCsvRow): MonoWebHookResponseData? {
        val account = mappings.getMonoAccIdByTelegramChatId(chatId) ?: return null
        val now = LocalDateTime.now()
        val statementItem = MonoStatementItem(
            row.hashCode().toString(),
            row.time
                .withHour(now.hour)
                .withMinute(now.minute)
                .toInstant(ZoneOffset.ofHours(0))
                .toKotlinInstant(),
            row.description,
            row.mcc,
            convertAmount(row.amount),
            convertAmount(row.operationAmount),
            row.currency,
            "",
            convertAmount(row.comission),
            convertAmount(row.cashbackAmount),
            convertAmount(row.balance),
            false
        )

        return MonoWebHookResponseData(account, statementItem)
    }

    private fun convertAmount(amount: Double): Long {
        val f = DecimalFormat("##.00")
        return f.format(amount).replace(".", "").toLong()
    }

    companion object {
        private const val RATE_LIMITING_BUFFER_SIZE = 100
    }
}
