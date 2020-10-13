package com.github.smaugfm.ynab

import io.ktor.client.features.*

class YnabApiException(
    val e: ResponseException,
    val errorResponse: YnabErrorResponse,
) : Exception(e)