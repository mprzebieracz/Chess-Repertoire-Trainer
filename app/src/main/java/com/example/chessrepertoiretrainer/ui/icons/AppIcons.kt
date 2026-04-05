package com.example.chessrepertoiretrainer.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Central place for semantic app icons. This makes it easy to later swap
 * out the concrete glyphs (e.g. to custom vector assets) without touching
 * every screen.
 */
object AppIcons {
    // Bottom bar / primary navigation
    val Home: ImageVector = Icons.Filled.Home
    // For now reuse the Home-style icon for repertoire; later this can
    // be swapped to a custom vector without touching call sites.
    val Repertoire: ImageVector = Icons.Filled.Home
    // Use a play arrow for training-related actions.
    val Train: ImageVector = Icons.Filled.PlayArrow
    val Puzzles: ImageVector = Icons.Filled.PlayArrow
    val OpeningTree: ImageVector = Icons.Filled.PlayArrow
    val Settings: ImageVector = Icons.Filled.Settings

    // Generic navigation
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack

    // Repertoires
    val AddRepertoire: ImageVector = Icons.Filled.Add
    val DeleteRepertoire: ImageVector = Icons.Filled.Delete

    // Course / chapter actions
    val TrainCourse: ImageVector = Icons.Filled.PlayArrow
    val EditCourse: ImageVector = Icons.Filled.Edit

    // Lines / chapter detail
    val TrainChapter: ImageVector = Icons.Filled.PlayArrow
    val ImportPgn: ImageVector = Icons.Filled.PlayArrow
    val AddLine: ImageVector = Icons.Filled.Add
    val DeleteLine: ImageVector = Icons.Filled.Delete
}



