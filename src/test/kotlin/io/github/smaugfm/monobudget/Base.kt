package io.github.smaugfm.monobudget

import io.mockk.mockkClass
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.test.junit5.mock.MockProviderExtension

open class Base : KoinTest {
    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        mockkClass(clazz)
    }
}
