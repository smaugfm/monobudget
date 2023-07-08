package io.github.smaugfm.monobudget.common.transaction

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.model.financial.Amount
import org.junit.jupiter.api.Test
import java.util.Currency

class TransactionMessageFormatterTest {
    @Test
    fun formatBudgetTest() {
        val (left, budget) = TransactionMessageFormatter.formatBudget(
            CategoryService.BudgetedCategory.CategoryBudget(
                Amount(637400, Currency.getInstance("UAH")),
                Amount(1500000, Currency.getInstance("UAH"))
            ),
            builder
        )
        assertThat(left).isEqualTo("6.4k")
        assertThat(budget).isEqualTo("15k UAH")
    }
}
