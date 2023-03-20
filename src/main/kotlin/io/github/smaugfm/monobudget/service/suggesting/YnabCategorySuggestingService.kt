package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory

class YnabCategorySuggestingService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    mccOverride: Settings.MccOverride,
    private val api: YnabApi
) : CategorySuggestingService(mccOverride) {
    private val categoriesFetcher = periodicFetcherFactory.create("") {
        api.getCategoryGroups().flatMap {
            it.categories
        }
    }

    override suspend fun categoryIdByName(categoryName: String): String? =
        categoriesFetcher.data
            .await()
            .firstOrNull { it.name == categoryName }
            ?.id
}
