package com.example.chessrepertoiretrainer.core.network

import java.net.HttpURLConnection
import java.net.URL

internal fun openGetConnection(
    url: String,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
    accept: String = "application/json"
): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        setRequestProperty("Accept", accept)
    }
