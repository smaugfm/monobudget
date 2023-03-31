package io.github.smaugfm.monobudget.lunchmoney

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

class LunchmoneyNewTransactionFactoryTest {

    @Test
    fun lunchmoneyAmount() {
        assertThat(
            LunchmoneyNewTransactionFactory.lunchmoneyAmount(
                499,
                Currency.getInstance("USD")
            )
        ).isEqualTo(BigDecimal("4.99"))
    }
}
