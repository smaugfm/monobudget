package io.github.smaugfm.monobudget.common.retry

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import kotlinx.datetime.Instant
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
    internal val objectMapper =
        jsonMapper {
            enable(SerializationFeature.INDENT_OUTPUT)
            addModule(kotlinModule())
            addModule(JavaTimeModule())
            addModule(
                SimpleModule().also {
                    it.addSerializer(KotlinxTimeInstantJacksonSerializer())
                    it.addDeserializer(Instant::class.java, KotlinxTimeInstantJacksonDeserializer())
                },
            )
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
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE,
        )
    }

    private class KotlinxTimeInstantJacksonSerializer : StdSerializer<Instant>(Instant::class.java) {
        override fun serialize(
            value: Instant?,
            gen: JsonGenerator?,
            provider: SerializerProvider?,
        ) {
            gen?.writeString(value?.toString())
        }
    }

    private class KotlinxTimeInstantJacksonDeserializer : StdDeserializer<Instant>(Instant::class.java) {
        override fun deserialize(
            p: JsonParser?,
            ctxt: DeserializationContext?,
        ): Instant = Instant.parse(p?.valueAsString!!)
    }
}
