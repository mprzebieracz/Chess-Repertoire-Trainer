package com.example.chessrepertoiretrainer.feature.mygames.domain.model

data class GameFilter(
    val timeCategory: String? = null,
    val playerResult: String? = null,
    val platform: String? = null,
    val isPlayerWhite: Boolean? = null
)
