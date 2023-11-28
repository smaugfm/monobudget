package io.github.smaugfm.monobudget

import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.KoinApplication
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.mock.MockProvider

@ExtendWith(MockKExtension::class)
@Suppress("MagicNumber")
open class TestBase : KoinTest {
    open fun testKoinApplication(app: KoinApplication) {
    }

    @Suppress("unused")
    @JvmField
    @RegisterExtension
    val koinTestExtension =
        KoinTestExtension.create {
            testKoinApplication(this@create)
        }

    @BeforeEach
    fun declareMockProvider() {
        MockProvider.register { mockkClass(it) }
    }
}
