package com.example.chessrepertoiretrainer.feature.mygames.domain.model

data class GameFilter(val platform: String? = null,
                      val isPlayerWhite: Boolean? = null,
                      val timeCategories: Set<String> = emptySet(),
                      val selectedResults: Set<String> = emptySet())