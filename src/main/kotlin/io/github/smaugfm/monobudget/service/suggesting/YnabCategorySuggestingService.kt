package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory

class YnabCategorySuggestingService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    mccOverride: Settings.MccOverride,
    private val api: YnabApi
) : CategorySuggestingService(mccOverride) {
    private val categoriesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) {
        api.getCategoryGroups().flatMap {
            it.categories
        }
    }

    override suspend fun categoryNameById(categoryId: String): String? =
        categoriesFetcher.getData().find { it.id == categoryId }?.name

    override suspend fun categoryIdByName(categoryName: String): String? =
        categoriesFetcher.getData()
            .firstOrNull { it.name == categoryName }
            ?.id
}
