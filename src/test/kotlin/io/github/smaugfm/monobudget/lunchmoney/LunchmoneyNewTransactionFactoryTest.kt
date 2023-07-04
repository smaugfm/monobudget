package io.github.smaugfm.monobudget.lunchmoney

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.smaugfm.monobudget.common.model.financial.Amount
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

class LunchmoneyNewTransactionFactoryTest {

    @Test
    fun lunchmoneyAmountTest() {
        assertThat(
            Amount(499).toLunchmoneyAmount(Currency.getInstance("USD"))
        ).isEqualTo(BigDecimal("4.99"))
    }
}
