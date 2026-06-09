package com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun createLineImageFile(context: Context, lineId: Int): File {
    val dir = File(context.filesDir, "line_images").also { it.mkdirs() }
    return File(dir, "line_$lineId.jpg")
}

fun lineImageFileProviderUri(context: Context, lineId: Int): Uri {
    val file = createLineImageFile(context, lineId)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun copyUriToLineImage(context: Context, uri: Uri, lineId: Int): String? {
    return try {
        val dest = createLineImageFile(context, lineId)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    } catch (e: Exception) {
        null
    }
}
