package com.example.chessrepertoiretrainer.feature.settings.presentation.state

data class SettingsUiState(val isSaving: Boolean = false,
                           val saveSuccessMessage: String? = null,
                           val saveErrorMessage: String? = null)