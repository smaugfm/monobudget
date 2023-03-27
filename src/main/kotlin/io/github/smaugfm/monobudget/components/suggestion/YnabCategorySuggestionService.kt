package io.github.smaugfm.monobudget.components.suggestion

import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory

class YnabCategorySuggestionService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    mccOverride: Settings.MccOverride,
    private val api: YnabApi
) : CategorySuggestionService(mccOverride) {
    private val categoriesFetcher = periodicFetcherFactory.create(this::class.simpleName!!) {
        api.getCategoryGroups().flatMap {
            it.categories
        }
    }

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> = categoriesFetcher.getData().map {
        it.id to it.name
    }

    override suspend fun categoryNameById(categoryId: String?): String? = if (categoryId == null) {
        null
    } else {
        categoriesFetcher.getData().find { it.id == categoryId }?.name
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
}
