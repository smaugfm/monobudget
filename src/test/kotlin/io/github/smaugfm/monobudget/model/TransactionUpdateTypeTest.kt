package io.github.smaugfm.monobudget.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class TransactionUpdateTypeTest {

    @Test
    fun extractIdFromMessageText() {
        assertThat(
            TransactionUpdateType.extractTransactionId(
                """ Нова транзакція Monobank додана в Lunchmoney
                💳 Інтернет-банк PUMBOnline
                      Банківський переказ грошових доручень / Грошові перекази (4829)
                      20.00UAH
                      Category: 
                      Payee:    Інтернет-банк PUMBOnline


                184370613 """.trimIndent()
            )
        ).isEqualTo("184370613")
    }
}
