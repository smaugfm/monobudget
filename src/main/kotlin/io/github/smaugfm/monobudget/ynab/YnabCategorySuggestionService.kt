package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.suggestion.CategorySuggestionService
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single
class YnabCategorySuggestionService : CategorySuggestionService() {
    private val periodicFetcherFactory: PeriodicFetcherFactory by inject()
    private val api: YnabApi by inject()

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
