package io.github.smaugfm.monobudget.integration

import io.github.smaugfm.monobudget.common.retry.JacksonFileStatementRetryRepository
import io.github.smaugfm.monobudget.common.retry.StatementRetryRepository
import org.junit.jupiter.api.BeforeEach
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import java.nio.file.Files
import java.nio.file.Paths

class JacksonRepositoryRetriesTest : RetriesTest() {
    private val repo = JacksonFileStatementRetryRepository(Paths.get("retries.json"))

    override fun testKoinApplication(app: KoinApplication) {
        super.testKoinApplication(app)
        app.modules(
            module {
                single { repo } bind StatementRetryRepository::class
            },
        )
    }

    @BeforeEach
    fun deleteFile() {
        Files.deleteIfExists(Paths.get("retries.json"))
    }
}
