package com.example.chessrepertoiretrainer.feature.settings.presentation.state

data class SettingsScreenState(
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null,
    val saveErrorMessage: String? = null
)

