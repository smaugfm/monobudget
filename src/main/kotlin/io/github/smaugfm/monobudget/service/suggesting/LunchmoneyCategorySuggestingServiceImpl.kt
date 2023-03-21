package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.request.category.LunchmoneyGetAllCategoriesRequest
import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import kotlinx.coroutines.reactor.awaitSingle

class LunchmoneyCategorySuggestingServiceImpl(
    periodicFetcherFactory: PeriodicFetcherFactory,
    mccOverride: Settings.MccOverride,
    private val api: LunchmoneyApi
) : CategorySuggestingService(mccOverride) {
    private val categoriesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) {
        api.execute(LunchmoneyGetAllCategoriesRequest())
            .awaitSingle()
            .categories
    }
    override suspend fun categoryNameById(categoryId: String): String? {
        val idLong = categoryId.toLong()
        return categoriesFetcher.getData().find { it.id == idLong }?.name
    }

    override suspend fun categoryIdByName(categoryName: String): String? =
        categoriesFetcher.getData()
            .firstOrNull { it.name == categoryName }
            ?.id
            ?.toString()
}
