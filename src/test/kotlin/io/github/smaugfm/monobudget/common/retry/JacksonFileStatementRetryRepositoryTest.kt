package io.github.smaugfm.monobudget.common.retry

import assertk.assertThat
import assertk.assertions.isEmpty
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.integration.RetriesTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.time.Duration

class JacksonFileStatementRetryRepositoryTest : RetriesTest() {
    private val repo = JacksonFileStatementRetryRepository(Paths.get("retries.json"))

    override fun testKoinApplication(app: KoinApplication) {
        super.testKoinApplication(app)
        app.modules(
            module {
                single { repo } bind StatementRetryRepository::class
            },
        )
    }

    @Test
    fun `Mono statement item serializes & deserializes correctly`() {
        val repo = JacksonFileStatementRetryRepository(Paths.get("retries.json"))
        runBlocking {
            val req1 = repo.addRetryRequest(StatementProcessingContext(statementItem1()), Duration.ZERO)
            val req2 = repo.addRetryRequest(StatementProcessingContext(statementItem1()), Duration.ZERO)
            repo.removeRetryRequest(req1.id)
            repo.removeRetryRequest(req2.id)
            assertThat(repo.getAllRequests()).isEmpty()
        }
    }

    @BeforeEach
    fun deleteFile() {
        Files.deleteIfExists(Paths.get("retries.json"))
    }
}
