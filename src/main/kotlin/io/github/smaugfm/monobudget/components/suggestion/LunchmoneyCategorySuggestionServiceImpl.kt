package io.github.smaugfm.monobudget.components.suggestion

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.request.category.LunchmoneyGetAllCategoriesRequest
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import kotlinx.coroutines.reactor.awaitSingle

class LunchmoneyCategorySuggestionServiceImpl(
    periodicFetcherFactory: PeriodicFetcherFactory,
    mccOverride: Settings.MccOverride,
    private val api: LunchmoneyApi
) : CategorySuggestionService(mccOverride) {
    private val categoriesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) {
        api.execute(LunchmoneyGetAllCategoriesRequest())
            .awaitSingle()
            .categories
    }
    override suspend fun categoryNameById(categoryId: String?): String? {
        if (categoryId == null) {
            return null
        }
        val idLong = categoryId.toLong()
        return categoriesFetcher.getData().find { it.id == idLong }?.name
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
        ?.toString()
}
