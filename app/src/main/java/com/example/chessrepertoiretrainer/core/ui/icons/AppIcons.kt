package com.example.chessrepertoiretrainer.core.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

object AppIcons {
    // Bottom bar navigation
    val Home: ImageVector = Icons.Filled.Home
    val Repertoire: ImageVector = Icons.AutoMirrored.Filled.MenuBook
    val Train: ImageVector = Icons.Filled.School
    val MyGames: ImageVector = Icons.Filled.AccountBox
    val OpeningTree: ImageVector = Icons.AutoMirrored.Filled.ManageSearch
    val Settings: ImageVector = Icons.Filled.Tune

    // General navigation
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack

    // Repertoire CRUD
    val AddRepertoire: ImageVector = Icons.Filled.Add
    val DeleteRepertoire: ImageVector = Icons.Filled.Delete
    val TrainCourse: ImageVector = Icons.Filled.School
    val EditCourse: ImageVector = Icons.Filled.Edit
    val TrainChapter: ImageVector = Icons.Filled.School
    val ImportPgn: ImageVector = Icons.Filled.Add
    val AddLine: ImageVector = Icons.Filled.Add
    val DeleteLine: ImageVector = Icons.Filled.Delete

    // Chess action buttons
    val FlipBoard: ImageVector = Icons.Filled.Flip
    val ResetBoard: ImageVector = Icons.Filled.RestartAlt
    val Hint: ImageVector = Icons.Filled.Lightbulb
    val Solution: ImageVector = Icons.Filled.Visibility
    val Engine: ImageVector = Icons.Filled.Psychology
    val SkipLine: ImageVector = Icons.Filled.SkipNext
    val DailyPuzzle: ImageVector = Icons.Filled.Casino
}