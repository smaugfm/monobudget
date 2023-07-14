package io.github.smaugfm.monobudget.common.transaction

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.model.financial.Amount
import org.junit.jupiter.api.Test
import java.util.Currency
import kotlin.math.roundToLong

class TransactionMessageFormatterTest {
    @Test
    fun formatBudgetTest() {
        val builder = StringBuilder()
        val a = 637400L
        val b = 1500000L
        val percent =
            "${(a.toDouble() * 100 / b).roundToLong()}%"
        TransactionMessageFormatter.formatBudget(
            CategoryService.BudgetedCategory.CategoryBudget(
                Amount(637400, Currency.getInstance("UAH")),
                Amount(1500000, Currency.getInstance("UAH"))
            ),
            builder
        )
        assertThat(builder.toString()).isEqualTo("Залишок: <code>₴6.4k із ₴15k</code> (<b>$percent</b>)")
    }
}
