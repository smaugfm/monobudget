package io.github.smaugfm.monobudget.common.retry

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration

class JacksonFileStatementRetryRepository(
    private val path: Path,
) : StatementRetryRepository {
    val objectMapper =
        jsonMapper {
            enable(SerializationFeature.INDENT_OUTPUT)
            addModule(kotlinModule())
            addModule(JavaTimeModule())
        }

    override suspend fun addRetryRequest(
        ctx: StatementProcessingContext,
        retryWaitDuration: Duration,
    ) = StatementRetryRequest(UUID.randomUUID().toString(), ctx, retryWaitDuration)
        .also {
            save(getAllRequests() + it)
        }

    override suspend fun removeRetryRequest(id: RetryRequestId) {
        save(getAllRequests().filter { it.id != id })
    }

    override suspend fun getAllRequests(): List<StatementRetryRequest> =
        if (path.exists()) {
            objectMapper.readValue(
                path.readText(),
                object : TypeReference<List<StatementRetryRequest>>() {},
            )
        } else {
            emptyList()
        }

    private fun save(list: List<StatementRetryRequest>) {
        path.writeText(
            objectMapper.writeValueAsString(list),
            Charsets.UTF_8,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE
        )
    }
}
