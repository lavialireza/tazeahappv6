package com.example.bookapp.ui

// این فایل فقط در بیلد Admin کامپایل می‌شود (app/src/admin/java).
// نسخه‌ی خالی/بدون‌کد همین تابع در app/src/viewer/java قرار دارد، بنابراین
// کدهای واقعی صفحات مدیریتی هرگز وارد خروجی APK نسخه Viewer نمی‌شوند
// (نه فقط پنهان یا obfuscate‌شده، بلکه اصلاً در آن variant کامپایل نمی‌شوند).

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.ContentImportPreview
import com.example.bookapp.data.ContentHealthReport
import com.example.bookapp.data.buildDetailedContentHealthReport
import com.example.bookapp.data.toPersianText
import com.example.bookapp.data.exportContentJson
import com.example.bookapp.data.importContentJson
import com.example.bookapp.data.previewContentImport
import com.example.bookapp.data.readJsonFromUri
import com.example.bookapp.data.WordImportPreview
import com.example.bookapp.data.importWordContent
import com.example.bookapp.data.previewWordImport
import com.example.bookapp.data.readWordFromUri
import com.example.bookapp.data.Prefs
import com.example.bookapp.data.syncLocalContentFiles
import com.example.bookapp.data.syncRemoteContent
import com.example.bookapp.ui.screens.*
import kotlinx.coroutines.launch

private const val ROUTE_VIEWER_ACCESS = "viewer_access"
private const val ROUTE_SPECIAL_USERS = "special_users"
private const val ROUTE_ACCESS_AUDIT = "access_audit"
private const val ROUTE_CONTENT_MANAGEMENT = "content_management"
private const val ROUTE_CONTENT_EDITOR = "content_editor"
private const val ROUTE_DIALOGUE_BUILDER = "dialogue_builder/{taziehId}/{taziehTitle}"

/**
 * مسیرهای Navigation که فقط مخصوص نسخه Admin هستند (مدیریت محتوا، ویرایشگر،
 * ورود JSON/Word، مدیریت دسترسی Viewer، کاربران ویژه، لاگ دسترسی).
 * فراخوانی این تابع از AppNavigation.kt مشترک انجام می‌شود، اما بدنه‌ی واقعی
 * آن (همین فایل) فقط عضو variant های Admin است.
 */
fun NavGraphBuilder.adminOnlyRoutes(
    navController: NavHostController,
    db: AppDatabase,
    context: android.content.Context
) {
        composable(ROUTE_VIEWER_ACCESS) {
            ViewerAccessManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_SPECIAL_USERS) {
            SpecialUsersManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_ACCESS_AUDIT) {
            AccessAuditLogScreen(onBack = { navController.popBackStack() })
        }

        composable(ROUTE_CONTENT_MANAGEMENT) {
            var fieldsCount by remember { mutableIntStateOf(0) }
            var taziehsCount by remember { mutableIntStateOf(0) }
            var rolesCount by remember { mutableIntStateOf(0) }
            var sectionsCount by remember { mutableIntStateOf(0) }
            var imagesCount by remember { mutableIntStateOf(0) }
            var dialoguesCount by remember { mutableIntStateOf(0) }
            var processedFilesCount by remember { mutableIntStateOf(0) }
            var warnings by remember { mutableStateOf(emptyList<String>()) }
            var busy by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf<String?>(null) }
            var importPreview by remember { mutableStateOf<ContentImportPreview?>(null) }
            var pendingImportJson by remember { mutableStateOf<String?>(null) }
            var wordPreview by remember { mutableStateOf<WordImportPreview?>(null) }
            var pendingWordBytes by remember { mutableStateOf<ByteArray?>(null) }
            var healthReport by remember { mutableStateOf<ContentHealthReport?>(null) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        val json = exportContentJson(db)
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                        message = "خروجی JSON سازگار با برنامه جانبی با موفقیت ذخیره شد."
                    } catch (e: Exception) {
                        message = "خطا در خروجی JSON: ${e.message ?: "خطای نامشخص"}"
                    } finally { busy = false }
                }
            }
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        val json = readJsonFromUri(context, uri)
                        val preview = previewContentImport(db, json)
                        pendingImportJson = if (preview.valid) json else null
                        importPreview = preview
                    } catch (e: Exception) {
                        pendingImportJson = null
                        importPreview = ContentImportPreview(false, listOf("خطا در خواندن فایل: ${e.message ?: "خطای نامشخص"}"))
                    } finally { busy = false }
                }
            }
            val wordImportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) scope.launch {
                    busy = true
                    try {
                        val bytes = readWordFromUri(context, uri)
                        val (preview, _) = previewWordImport(db, bytes)
                        pendingWordBytes = if (preview.valid) bytes else null
                        wordPreview = preview
                    } catch (e: Exception) {
                        pendingWordBytes = null
                        wordPreview = WordImportPreview(false, listOf("خطا در خواندن فایل Word: ${e.message ?: "خطای نامشخص"}"))
                    } finally { busy = false }
                }
            }

            suspend fun reload() {
                val fields = db.fieldDao().getAll()
                val taziehs = db.taziehDao().getAll()
                val roles = taziehs.flatMap { db.roleDao().getByTazieh(it.id) }
                val sections = db.sectionDao().getAll()
                val images = taziehs.sumOf { db.taziehImageDao().getByTazieh(it.id).size }
                val dialogues = taziehs.sumOf { db.dialogueDao().getByTazieh(it.id).size }
                fieldsCount = fields.size
                taziehsCount = taziehs.size
                rolesCount = roles.size
                sectionsCount = sections.size
                imagesCount = images
                dialoguesCount = dialogues
                processedFilesCount = Prefs.getProcessedContentFiles(context).size
                warnings = buildList {
                    if (fields.any { it.title.isBlank() }) add("یک یا چند زمینه بدون عنوان است")
                    if (taziehs.any { it.title.isBlank() }) add("یک یا چند تعزیه بدون عنوان است")
                    if (roles.any { it.title.isBlank() }) add("یک یا چند نقش بدون عنوان است")
                    if (sections.any { it.content.isBlank() }) add("یک یا چند بخش بدون متن است")
                    if (fields.any { it.uid.isBlank() } || taziehs.any { it.uid.isBlank() } || roles.any { it.uid.isBlank() } || sections.any { it.uid.isBlank() }) add("یک یا چند رکورد شناسه پایدار ندارد")
                }
            }
            LaunchedEffect(Unit) { reload() }

            ContentManagementScreen(
                fieldsCount = fieldsCount,
                taziehsCount = taziehsCount,
                rolesCount = rolesCount,
                sectionsCount = sectionsCount,
                imagesCount = imagesCount,
                dialoguesCount = dialoguesCount,
                healthWarnings = warnings,
                processedFilesCount = processedFilesCount,
                busy = busy,
                message = message,
                onRefresh = { scope.launch { reload() } },
                onSyncLocal = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val count = syncLocalContentFiles(context, db)
                            reload()
                            message = if (count == 0) "محتوای جدیدی برای اضافه‌کردن وجود ندارد." else "$count فایل محتوایی بررسی و به‌روزرسانی شد."
                        } catch (e: Exception) {
                            message = "خطا در به‌روزرسانی محتوا: ${e.message ?: "خطای نامشخص"}"
                        } finally { busy = false }
                    }
                },
                onSyncRemote = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val result = syncRemoteContent(db)
                            reload()
                            message = result.fold({ "محتوای آنلاین با موفقیت همگام شد." }, { "خطا در همگام‌سازی آنلاین: ${it.message ?: "خطای نامشخص"}" })
                        } finally { busy = false }
                    }
                },
                onOpenEditor = { navController.navigate(ROUTE_CONTENT_EDITOR) },
                onImportJson = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                onExportJson = { exportLauncher.launch("tazieh-content-compatible.json") },
                onImportWord = { wordImportLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream")) },
                onDetailedHealthCheck = { scope.launch { busy = true; try { healthReport = buildDetailedContentHealthReport(db) } finally { busy = false } } },
                onExportHealthReport = {
                    scope.launch {
                        busy = true
                        try {
                            val report = healthReport ?: buildDetailedContentHealthReport(db).also { healthReport = it }
                            val text = report.toPersianText()
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "گزارش سلامت محتوا")
                            }, "اشتراک‌گذاری گزارش"))
                        } finally {
                            busy = false
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )

            val preview = importPreview
            if (preview != null) {
                AlertDialog(
                    onDismissRequest = { importPreview = null; pendingImportJson = null },
                    title = { Text(if (preview.valid) "پیش‌نمایش ورود JSON" else "فایل JSON نامعتبر") },
                    text = {
                        if (!preview.valid) {
                            Column { preview.errors.take(10).forEach { Text("• $it") } }
                        } else {
                            Column {
                                Text("قالب دقیق برنامه جانبی تأیید شد.")
                                Spacer(Modifier.height(8.dp))
                                Text("زمینه: ${preview.fields}")
                                Text("تعزیه: ${preview.taziehs}")
                                Text("نقش: ${preview.roles}")
                                Text("بخش: ${preview.sections}")
                                Spacer(Modifier.height(8.dp))
                                Text("موجود/قابل‌به‌روزرسانی: ${preview.existingItems}")
                                Text("جدید: ${preview.newItems}")
                                Spacer(Modifier.height(8.dp))
                                Text("فقط ساختار محتوا و متن بخش‌ها وارد می‌شود؛ اطلاعات شخصی، یادداشت‌ها، بوکمارک‌ها، تصاویر و گفتگوهای محلی دست‌کاری نمی‌شوند.")
                            }
                        }
                    },
                    confirmButton = {
                        if (preview.valid && pendingImportJson != null) {
                            TextButton(onClick = {
                                val json = pendingImportJson
                                importPreview = null
                                pendingImportJson = null
                                if (json != null) scope.launch {
                                    busy = true
                                    try {
                                        val result = importContentJson(db, json)
                                        reload()
                                        message = "ورود JSON با موفقیت انجام شد: ${result.newItems} مورد جدید و ${result.existingItems} مورد موجود/قابل‌به‌روزرسانی."
                                    } catch (e: Exception) {
                                        message = "خطا در ورود JSON: ${e.message ?: "خطای نامشخص"}"
                                    } finally { busy = false }
                                }
                            }) { Text("تأیید و ورود") }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { importPreview = null; pendingImportJson = null }) { Text("انصراف") }
                    }
                )
            }

            val wp = wordPreview
            if (wp != null) {
                AlertDialog(
                    onDismissRequest = { wordPreview = null; pendingWordBytes = null },
                    title = { Text(if (wp.valid) "پیش‌نمایش ورود Word" else "فایل Word نامعتبر") },
                    text = {
                        if (!wp.valid) {
                            Column { wp.errors.take(10).forEach { Text("• $it") } }
                        } else {
                            Column {
                                Text("ساختار Word مطابق قرارداد ورود محتوای برنامه شناسایی شد.")
                                Spacer(Modifier.height(8.dp))
                                Text("Heading 1 → زمینه")
                                Text("Heading 2 → تعزیه")
                                Text("Heading 3 → نقش")
                                Text("Heading 4 → بخش")
                                Text("پاراگراف‌های معمولی زیر Heading 4 → متن بخش")
                                Spacer(Modifier.height(8.dp))
                                Text("زمینه: ${wp.fields} | تعزیه: ${wp.taziehs}")
                                Text("نقش: ${wp.roles} | بخش: ${wp.sections}")
                                Text("پاراگراف‌های متن: ${wp.paragraphs}")
                                Text("موجود/قابل‌به‌روزرسانی: ${wp.existingItems}")
                                Text("جدید: ${wp.newItems}")
                                Spacer(Modifier.height(8.dp))
                                Text("فقط ساختار محتوا و متن وارد می‌شود؛ یادداشت‌ها، نشانک‌ها، تصاویر، گفتگوها و اطلاعات شخصی تغییر نمی‌کنند.")
                            }
                        }
                    },
                    confirmButton = {
                        if (wp.valid && pendingWordBytes != null) {
                            TextButton(onClick = {
                                val bytes = pendingWordBytes
                                wordPreview = null
                                pendingWordBytes = null
                                if (bytes != null) scope.launch {
                                    busy = true
                                    try {
                                        val result = importWordContent(db, bytes)
                                        reload()
                                        message = "ورود Word با موفقیت انجام شد: ${result.newItems} مورد جدید و ${result.existingItems} مورد موجود/قابل‌به‌روزرسانی."
                                    } catch (e: Exception) {
                                        message = "خطا در ورود Word: ${e.message ?: "خطای نامشخص"}"
                                    } finally { busy = false }
                                }
                            }) { Text("تأیید و ورود") }
                        }
                    },
                    dismissButton = { TextButton(onClick = { wordPreview = null; pendingWordBytes = null }) { Text("انصراف") } }
                )
            }

            val hr = healthReport
            if (hr != null) {
                AlertDialog(
                    onDismissRequest = { healthReport = null },
                    title = { Text("گزارش عمیق سلامت محتوا") },
                    text = {
                        Column(Modifier.heightIn(max = 520.dp)) {
                            Text("بررسی‌شده: ${hr.checkedFields} زمینه، ${hr.checkedTaziehs} تعزیه، ${hr.checkedRoles} نقش، ${hr.checkedSections} بخش")
                            Spacer(Modifier.height(8.dp))
                            Text("بدون عنوان: ${hr.emptyTitles} | بدون متن: ${hr.emptyTexts}")
                            Text("عنوان تکراری: ${hr.duplicateTitles} | مشکل ترتیب: ${hr.orderIssues}")
                            Text("آدرس صوت مشکوک: ${hr.invalidAudio}")
                            Spacer(Modifier.height(10.dp))
                            if (hr.errors.isEmpty() && hr.warnings.isEmpty()) {
                                Text("✓ هیچ مورد قابل‌توجهی پیدا نشد.", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            } else {
                                if (hr.errors.isNotEmpty()) {
                                    Text("خطاها", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    hr.errors.take(12).forEach { Text("• $it", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
                                }
                                if (hr.warnings.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("هشدارها", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    hr.warnings.take(16).forEach { Text("• $it", style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { healthReport = null }) { Text("بستن") } }
                )
            }
        }

        composable(ROUTE_CONTENT_EDITOR) {
            ContentEditorScreen(
                db = db,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_DIALOGUE_BUILDER) { backStackEntry ->
            val taziehId = backStackEntry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = backStackEntry.arguments?.getString("taziehTitle") ?: ""
            var allSections by remember { mutableStateOf(listOf<SectionPickerItem>()) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            LaunchedEffect(taziehId) {
                val roles = db.roleDao().getByTazieh(taziehId)
                allSections = roles.flatMap { role ->
                    db.sectionDao().getByRole(role.id).map { section ->
                        SectionPickerItem(section.id, role.title, section.title)
                    }
                }
            }

            DialogueBuilderScreen(
                allSections = allSections,
                onSave = { title, orderedSectionIds ->
                    scope.launch {
                        val dialogueId = db.dialogueDao().insert(com.example.bookapp.data.DialogueEntity(taziehId = taziehId, title = title))
                        orderedSectionIds.forEachIndexed { index, sectionId ->
                            db.dialogueTurnDao().insert(
                                com.example.bookapp.data.DialogueTurnEntity(dialogueId = dialogueId, sectionId = sectionId, orderIndex = index)
                            )
                        }
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

}
