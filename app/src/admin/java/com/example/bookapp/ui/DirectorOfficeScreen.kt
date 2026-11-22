package com.example.bookapp.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.room.RoomDatabase
import com.example.bookapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

private suspend fun importDirectorSource(context: Context, uri: Uri, planId: Long): DirectorSourceFileEntity {
    val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("فایل قابل خواندن نیست") }
    require(bytes.isNotEmpty()) { "فایل خالی است" }
    require(bytes.size <= 35 * 1024 * 1024) { "حجم فایل بیش از ۳۵ مگابایت است" }
    val name = queryDisplayName(context, uri) ?: "director-source-${System.currentTimeMillis()}"
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val ext = name.substringAfterLast('.', "").lowercase()
    val dir = File(context.filesDir, "director_sources/$planId").apply { mkdirs() }
    val target = File(dir, "${UUID.randomUUID()}-${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
    target.writeBytes(bytes)
    val extracted = when (ext) {
        "txt", "text", "json" -> bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        "docx" -> wordToCompatibleJson(bytes)
        "pdf" -> runCatching { extractPdfText(context, bytes).text }.getOrElse { "" }
        else -> ""
    }
    return DirectorSourceFileEntity(planId = planId, originalName = name, mimeType = mime, localPath = target.absolutePath, sha256 = sha256(bytes), importMethod = when (ext) { "txt", "text", "json" -> "TEXT"; "docx" -> "DOCX_XML"; "pdf" -> "PDF_TEXT_OR_SCAN"; "jpg", "jpeg", "png" -> "IMAGE_OCR_PENDING"; else -> "SOURCE_ONLY" }, extractedText = extracted, createdAt = System.currentTimeMillis())
}


private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

@Composable
fun DirectorOfficeScreen(
    db: AppDatabase,
    initialTaziehId: Long? = null,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoDarkMode: Boolean,
    onAutoDarkModeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    themeChoice: String,
    onThemeChoiceChange: (String) -> Unit,
    fontChoice: String,
    onFontChoiceChange: (String) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val officeDao = remember(db) { db.directorDao() }
    val transactionRunner = remember(db) { RoomDirectorOfficeTransactionRunner(db) }
    val sourceImporter = remember {
        DirectorOfficeSourceImporter { context, uri, planId ->
            importDirectorSource(context, uri, planId)
        }
    }
    DirectorOfficeSharedMainScreen(
        libraryDb = db,
        officeDao = officeDao,
        transactionRunner = transactionRunner,
        isAdmin = true,
        sourceImporter = sourceImporter,
        initialTaziehId = initialTaziehId,
        darkMode = darkMode,
        onDarkModeChange = onDarkModeChange,
        autoDarkMode = autoDarkMode,
        onAutoDarkModeChange = onAutoDarkModeChange,
        fontScale = fontScale,
        onFontScaleChange = onFontScaleChange,
        themeChoice = themeChoice,
        onThemeChoiceChange = onThemeChoiceChange,
        fontChoice = fontChoice,
        onFontChoiceChange = onFontChoiceChange,
        keepScreenOn = keepScreenOn,
        onKeepScreenOnChange = onKeepScreenOnChange,
        onBack = onBack
    )
}
