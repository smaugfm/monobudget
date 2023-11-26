package io.github.smaugfm.monobudget.common.retry

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration

class JacksonFileStatementRetryRepository(
    private val path: Path
) : StatementRetryRepository {

    private val objectMapper = jacksonObjectMapper()

    override suspend fun addRetryRequest(
        ctx: StatementProcessingContext,
        retryWaitDuration: Duration
    ) = StatementRetryRequest(UUID.randomUUID().toString(), ctx, retryWaitDuration)
        .also {
            save(getAllRequests() + it)
        }

    override suspend fun removeRetryRequest(id: RetryRequestId) {
        save(getAllRequests().filter { it.id != id })
    }

    override suspend fun getAllRequests(): List<StatementRetryRequest> =
        objectMapper.readValue(
            path.readText(), object : TypeReference<List<StatementRetryRequest>>() {}
        )

    private fun save(list: List<StatementRetryRequest>) {
        path.writeText(objectMapper.writeValueAsString(list))
    }
}
