package com.example.bookapp.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.example.bookapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

@Composable
fun ViewerDirectorOfficeScreen(
    contentDb: AppDatabase,
    taziehId: Long? = null,
    policyPermissions: Map<String, Boolean>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val personalDb = remember { ViewerDirectorOfficeDatabase.personal(context) }
    val viewerDao = remember(personalDb, policyPermissions) {
        ViewerDirectorOfficeMainPageDao(personalDb.directorDao(), policyPermissions)
    }
    val transactionRunner = remember(personalDb) { RoomDirectorOfficeTransactionRunner(personalDb) }
    var darkMode by remember { mutableStateOf(Prefs.isDarkMode(context)) }
    var autoDarkMode by remember { mutableStateOf(Prefs.getAutoDarkMode(context)) }
    var fontScale by remember { mutableStateOf(Prefs.getFontScale(context)) }
    var fontChoice by remember { mutableStateOf(Prefs.getFontChoice(context)) }
    var themeChoice by remember { mutableStateOf(Prefs.getThemeChoice(context)) }
    var keepScreenOn by remember { mutableStateOf(Prefs.getKeepScreenOn(context)) }

    DisposableEffect(localView, keepScreenOn) {
        localView.keepScreenOn = keepScreenOn
        onDispose { localView.keepScreenOn = false }
    }

    val sourceImporter = remember {
        DirectorOfficeSourceImporter { sourceContext, uri, planId ->
            importViewerDirectorSource(sourceContext, uri, planId)
        }
    }

    DirectorOfficeSharedMainScreen(
        libraryDb = contentDb,
        officeDao = viewerDao,
        transactionRunner = transactionRunner,
        isAdmin = false,
        policyPermissions = policyPermissions,
        sourceImporter = sourceImporter,
        initialTaziehId = taziehId,
        darkMode = darkMode,
        onDarkModeChange = { darkMode = it; Prefs.setDarkMode(context, it) },
        autoDarkMode = autoDarkMode,
        onAutoDarkModeChange = { autoDarkMode = it; Prefs.setAutoDarkMode(context, it) },
        fontScale = fontScale,
        onFontScaleChange = { fontScale = it; Prefs.setFontScale(context, it) },
        themeChoice = themeChoice,
        onThemeChoiceChange = { themeChoice = it; Prefs.setThemeChoice(context, it) },
        fontChoice = fontChoice,
        onFontChoiceChange = { fontChoice = it; Prefs.setFontChoice(context, it) },
        keepScreenOn = keepScreenOn,
        onKeepScreenOnChange = { keepScreenOn = it; Prefs.setKeepScreenOn(context, it) },
        onBack = onBack
    )
}

/** واردکردن منبع در Viewer با همان صفحه و ثبت در پایگاه داده مستقل شخصی. */
private suspend fun importViewerDirectorSource(
    context: Context,
    uri: Uri,
    planId: Long
): DirectorSourceFileEntity = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("فایل قابل خواندن نیست")
    require(bytes.isNotEmpty()) { "فایل خالی است" }
    require(bytes.size <= 35 * 1024 * 1024) { "حجم فایل بیش از ۳۵ مگابایت است" }
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: "director-source-${System.currentTimeMillis()}"
    val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val ext = name.substringAfterLast('.', "").lowercase()
    val dir = File(context.filesDir, "director_sources/$planId").apply { mkdirs() }
    val target = File(dir, "${UUID.randomUUID()}-${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
    target.writeBytes(bytes)
    val extracted = when (ext) {
        "txt", "text", "json" -> bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        "docx" -> extractViewerDocxText(bytes)
        // Viewer فایل PDF را نگه می‌دارد؛ استخراج PDF همچنان در importer تخصصی Admin انجام می‌شود.
        else -> ""
    }
    val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    DirectorSourceFileEntity(
        planId = planId, originalName = name, mimeType = mime, localPath = target.absolutePath,
        sha256 = hash,
        importMethod = when (ext) {
            "txt", "text", "json" -> "TEXT"
            "docx" -> "DOCX_XML_TEXT"
            "pdf" -> "PDF_SOURCE_ONLY"
            "jpg", "jpeg", "png" -> "IMAGE_OCR_PENDING"
            else -> "SOURCE_ONLY"
        },
        extractedText = extracted, createdAt = System.currentTimeMillis()
    )
}

private fun extractViewerDocxText(bytes: ByteArray): String {
    val xml = ZipInputStream(bytes.inputStream()).use { zip ->
        var document = ""
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.name == "word/document.xml") {
                document = zip.readBytes().toString(Charsets.UTF_8)
                break
            }
        }
        document
    }
    if (xml.isBlank()) return ""
    return xml.replace(Regex("</w:p>"), "\n")
        .replace(Regex("<w:tab[^>]*/>"), "\t")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")
        .trim()
}
