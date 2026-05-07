package com.example.chessrepertoiretrainer.feature.mygames.domain.model

enum class StatsTimeRange(val label: String, val days: Int?) {
    DAYS_7("7d", 7),
    DAYS_30("30d", 30),
    DAYS_90("90d", 90),
    DAYS_365("1y", 365),
    ALL_TIME("All", null)
}

fun StatsTimeRange.sinceEpochMs(): Long =
    if (days == null) 0L else System.currentTimeMillis() - days * 86_400_000L
