package com.example.chessrepertoiretrainer.core.sound

enum class MoveSoundType { MOVE, CAPTURE, CHECK, MATE }

object MoveSoundClassifier {
    fun fromSan(san: String): MoveSoundType = when {
        san.endsWith("#") -> MoveSoundType.MATE
        san.endsWith("+") -> MoveSoundType.CHECK
        san.contains("x") -> MoveSoundType.CAPTURE
        else -> MoveSoundType.MOVE
    }
}
