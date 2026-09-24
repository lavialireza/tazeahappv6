package com.example.bookapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookapp.BuildConfig
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.UpdateHelper
import com.example.bookapp.data.Prefs
import com.example.bookapp.data.UpdateHistoryStore
import com.example.bookapp.data.SyncServerHelper
import kotlinx.coroutines.launch

private const val APP_WEBSITE = "" // آدرس واقعی سایت برنامه را اینجا وارد کنید؛ آدرس مخزن GitHub نباید در معرفی عمومی نمایش داده شود.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    fieldsCount: Int,
    taziehsCount: Int,
    rolesCount: Int,
    sectionsCount: Int,
    readCount: Int,
    streakDays: Int,
    activeDaysLast14: List<Boolean> = emptyList(),
    showAppIntro: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("درباره برنامه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "این اپلیکیشن یک کتابخانه دیجیتال از متون تعزیه است که بر اساس " +
                        "زمینه، تعزیه، نقش و بخش دسته‌بندی شده است."
            )
            Spacer(Modifier.height(20.dp))
            Text("آمار مجموعه:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("$fieldsCount زمینه")
            Text("$taziehsCount تعزیه")
            Text("$rolesCount نقش")
            Text("$sectionsCount بخش")

            Spacer(Modifier.height(20.dp))
            Text("آمار مطالعه شما:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("$readCount بخش را تا الان خوانده‌اید")
            if (streakDays > 1) {
                Text("$streakDays روز متوالی سر زده‌اید 🔥")
            }
            if (activeDaysLast14.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("۱۴ روز اخیر:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    activeDaysLast14.forEach { active ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("مشخصات نویسنده برنامه و گردآوری", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text("مهندس علیرضا لاوی")
            Text("نسخه: ${BuildConfig.VERSION_NAME}")
            Text("راه ارتباطی: [09132383677]")

            if (showAppIntro) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        buildString {
                            append("این اپ رو ببین: «تعزیه و شبیه‌خوانی» — کتابخانه‌ای کامل و آفلاین از نسخه‌های تعزیه.")
                            if (APP_WEBSITE.isNotBlank()) append("\n$APP_WEBSITE")
                        }
                    )
                }
                context.startActivity(android.content.Intent.createChooser(intent, "معرفی اپ به دیگران"))
                }) {
                    Text("معرفی این اپ به دیگران")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoDarkMode: Boolean,
    onAutoDarkModeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    fontChoice: String,
    onFontChoiceChange: (String) -> Unit,
    themeChoice: String,
    onThemeChoiceChange: (String) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    showContentSync: Boolean = true,
    showViewerAccessImport: Boolean = false,
    onImportViewerAccess: suspend (android.net.Uri) -> Result<String> = { Result.success("") },
    onSyncContent: suspend () -> Result<Unit>,
    onCheckAppUpdate: suspend () -> Result<UpdateHelper.UpdateInfo?> = { Result.success(null) },
    showUpdateManifestTools: Boolean = false,
    showSyncServerSettings: Boolean = false,
    syncServerUrl: String = "",
    onSaveSyncServerUrl: (String) -> Unit = {},
    onSyncServer: suspend () -> Result<SyncServerHelper.SyncReport> = { Result.failure(IllegalStateException("همگام‌سازی تنظیم نشده است")) },
    db: AppDatabase,
    onBack: () -> Unit
) {
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("حالت شب (تیره)", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange, enabled = !autoDarkMode)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("تاریک خودکار بر اساس ساعت", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "بین ساعت ۱۸ تا ۶ صبح خودکار به حالت تیره می‌رود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoDarkMode, onCheckedChange = onAutoDarkModeChange)
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("روشن نگه‌داشتن صفحه", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "صفحه گوشی حین استفاده از برنامه خاموش/قفل نشود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChange)
            }

            Spacer(Modifier.height(24.dp))

            Text("سایز متن", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "${(fontScale * 100).toInt()}٪",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Slider(
                value = fontScale.coerceIn(0.8f, 2.0f),
                onValueChange = onFontScaleChange,
                valueRange = 0.8f..2.0f,
                steps = 11,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("فونت متن", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.bookapp.ui.theme.FontChoiceLabels.forEach { (key, label) ->
                    ThemeOption(label, key, fontChoice, onFontChoiceChange)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("فاصله خطوط متن", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            var lineSpacing by remember { mutableStateOf(Prefs.getLineSpacing(context)) }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LineSpacingOption("فشرده", 1.1f, lineSpacing) { lineSpacing = it; Prefs.setLineSpacing(context, it) }
                LineSpacingOption("معمولی", 1.4f, lineSpacing) { lineSpacing = it; Prefs.setLineSpacing(context, it) }
                LineSpacingOption("بازتر", 1.8f, lineSpacing) { lineSpacing = it; Prefs.setLineSpacing(context, it) }
            }

            Spacer(Modifier.height(16.dp))
            Text("پیش‌نمایش تغییرات", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            val previewFontFamily = com.example.bookapp.ui.theme.FontChoices[fontChoice]
                ?: com.example.bookapp.ui.theme.FontChoices["titr"]!!
            val previewFontSize = 18f * fontScale.coerceIn(0.8f, 2.0f)
            val previewLineHeight = previewFontSize * lineSpacing
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    "نمونه متن تعزیه: این نوشته برای مشاهده هم‌زمان اندازه فونت، نوع فونت و فاصله خطوط است. با تغییر هر گزینه، این پیش‌نمایش نیز بلافاصله تغییر می‌کند.",
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = previewFontFamily,
                        fontSize = previewFontSize.sp,
                        lineHeight = previewLineHeight.sp
                    )
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("تغییر رمز عبور", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "اگر رمزی تنظیم نکنید، ورود به برنامه بدون رمز آزاد خواهد بود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            ChangePasswordSection()

            Spacer(Modifier.height(24.dp))
            Text("تم رنگی", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption("طلایی", "default", themeChoice, onThemeChoiceChange, androidx.compose.ui.graphics.Color(0xFFD4A94A))
                ThemeOption("سبز", "green", themeChoice, onThemeChoiceChange, androidx.compose.ui.graphics.Color(0xFF3E8E5A))
                ThemeOption("قرمز", "red", themeChoice, onThemeChoiceChange, androidx.compose.ui.graphics.Color(0xFFA33B3B))
            }

            if (showSyncServerSettings) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("همگام‌سازی با لپ‌تاپ / Web", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "آدرس Sync Server را وارد کنید. برنامه بدون فشردن دکمه همگام‌سازی به اینترنت وصل نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                var serverUrl by remember(syncServerUrl) { mutableStateOf(syncServerUrl) }
                var syncBusy by remember { mutableStateOf(false) }
                var syncResult by remember { mutableStateOf<String?>(null) }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("آدرس Sync Server") },
                    placeholder = { Text("http://192.168.1.10:8091") }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSaveSyncServerUrl(serverUrl) }) { Text("ذخیره آدرس") }
                    OutlinedButton(
                        enabled = !syncBusy,
                        onClick = {
                            syncBusy = true
                            syncResult = null
                            scope.launch {
                                val r = onSyncServer()
                                syncBusy = false
                                syncResult = r.fold(
                                    { "همگام‌سازی انجام شد. ${it.uploadedSections} بخش ارسال شد${if (it.pulledNewSections > 0) " و ${it.pulledNewSections} بخش جدید دریافت شد" else ""}." },
                                    { "خطا: ${it.message ?: "اتصال به Sync Server ناموفق بود"}" }
                                )
                            }
                        }
                    ) { Text(if (syncBusy) "در حال همگام‌سازی…" else "همگام‌سازی اکنون") }
                }
                syncResult?.let { Text(it, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("بروزرسانی برنامه", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "استفاده عادی برنامه بدون اینترنت است. با این دکمه فقط در صورت اتصال اینترنت، نسخه جدید بررسی می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            var checkingAppUpdate by remember { mutableStateOf(false) }
            var downloadingAppUpdate by remember { mutableStateOf(false) }
            var downloadPercent by remember { mutableStateOf(0) }
            var appUpdateMessage by remember { mutableStateOf<String?>(null) }
            Button(
                onClick = {
                    if (checkingAppUpdate || downloadingAppUpdate) return@Button
                    checkingAppUpdate = true
                    appUpdateMessage = null
                    scope.launch {
                        val result = onCheckAppUpdate()
                        checkingAppUpdate = false
                        result.fold(
                            onSuccess = { info ->
                                if (info == null) {
                                    appUpdateMessage = "برنامه شما به‌روز است. نسخه فعلی: ${com.example.bookapp.data.UpdateHelper.getInstalledVersion(context).versionName}"
                                } else {
                                    downloadingAppUpdate = true
                                    downloadPercent = 0
                                    val notes = if (info.releaseNotes.isEmpty()) "" else "\nتغییرات:\n• " + info.releaseNotes.joinToString("\n• ")
                                    appUpdateMessage = "نسخه فعلی: ${com.example.bookapp.data.UpdateHelper.getInstalledVersion(context).versionName}\nنسخه جدید: ${info.versionName} (build ${info.buildNumber})\n${if (info.forceUpdate) "این بروزرسانی اجباری است.\n" else ""}${if (info.minSupportedVersion > 0) "حداقل نسخه مجاز: ${info.minSupportedVersion}\n" else ""}${if (info.releaseDate.isNotBlank()) "تاریخ انتشار: ${info.releaseDate}\n" else ""}دریافت نسخه جدید داخل برنامه آغاز شد.$notes"
                                    val downloadResult = com.example.bookapp.data.UpdateHelper.downloadAndInstall(
                                        context,
                                        info
                                    ) { percent ->
                                        downloadPercent = percent
                                    }
                                    downloadingAppUpdate = false
                                    downloadResult.onSuccess {
                                        com.example.bookapp.data.UpdateHistoryStore.record(context, com.example.bookapp.data.UpdateHelper.getInstalledVersion(context).buildNumber, info.buildNumber, true, "درخواست نصب با موفقیت انجام شد")
                                    }
                                    downloadResult.onFailure { error ->
                                        com.example.bookapp.data.UpdateHistoryStore.record(context, com.example.bookapp.data.UpdateHelper.getInstalledVersion(context).buildNumber, info.buildNumber, false, error.message ?: "دریافت/نصب ناموفق")
                                        appUpdateMessage = error.message ?: "دریافت بروزرسانی ناموفق بود."
                                    }
                                }
                            },
                            onFailure = {
                                appUpdateMessage = "بررسی بروزرسانی ناموفق بود: ${it.message ?: "اتصال اینترنت را بررسی کنید"}"
                            }
                        )
                    }
                },
                enabled = !checkingAppUpdate && !downloadingAppUpdate
            ) {
                Text(
                    when {
                        checkingAppUpdate -> "در حال بررسی..."
                        downloadingAppUpdate -> "در حال دریافت ${downloadPercent}%..."
                        else -> "بررسی بروزرسانی برنامه"
                    }
                )
            }
            if (downloadingAppUpdate) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            appUpdateMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Text("تاریخچه بروزرسانی", style = MaterialTheme.typography.titleSmall)
            UpdateHistoryStore.get(context).take(8).forEach { h ->
                Text("${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(h.at))} — ${h.fromVersion} → ${h.toVersion} — ${if (h.success) "موفق" else "ناموفق"}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
            if (showContentSync) {
                            Text("بروزرسانی محتوا", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "اگر محتوای جدیدی به گیت‌هاب اضافه شده، با این دکمه بدون نیاز به نصب دوباره اپ، محتوا به‌روز می‌شود (نیاز به اینترنت دارد).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    syncing = true
                                    syncMessage = null
                                    scope.launch {
                                        val result = onSyncContent()
                                        syncing = false
                                        syncMessage = if (result.isSuccess) {
                                            com.example.bookapp.data.showNewContentNotification(context, 1)
                                            "محتوا با موفقیت به‌روزرسانی شد ✅"
                                        } else {
                                            "خطا در بروزرسانی: ${result.exceptionOrNull()?.message ?: "اتصال اینترنت را بررسی کنید"} ❌"
                                        }
                                    }
                                },
                                enabled = !syncing
                            ) {
                                Text(if (syncing) "در حال بروزرسانی..." else "بروزرسانی محتوا از اینترنت")
                            }
                            syncMessage?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }

            }

            if (showViewerAccessImport) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("وضعیت دسترسی این Viewer", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                val viewerInstallationId = com.example.bookapp.data.ViewerAccessPolicy.installationId(context)
                val effectivePermissions = com.example.bookapp.data.ViewerAccessPolicy.getEffectivePermissions(context)
                val specialMatch = com.example.bookapp.data.ViewerAccessPolicy.getSpecialUsers(context)
                    .firstOrNull { it.installationId == viewerInstallationId }
                val accessSource = if (specialMatch != null && (specialMatch.expiresAt == null || specialMatch.expiresAt <= 0L || System.currentTimeMillis() <= specialMatch.expiresAt)) {
                    "کاربر خاص"
                } else {
                    "پروفایل عمومی"
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("شناسه نصب: $viewerInstallationId", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "این شناسه را برای مدیر/ادمین ارسال کنید تا دسترسی اختصاصی برای همین Viewer صادر شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            "شناسه نصب Viewer من: $viewerInstallationId\n\nلطفاً این شناسه را برای صدور دسترسی اختصاصی استفاده کنید."
                                        )
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "ارسال شناسه نصب"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ارسال شناسه")
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(
                                        android.content.ClipData.newPlainText("شناسه نصب Viewer", viewerInstallationId)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("کپی شناسه")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("منبع دسترسی: $accessSource", style = MaterialTheme.typography.bodyMedium)
                        Text("قابلیت‌های فعال: ${effectivePermissions.count { it.value }} از ${effectivePermissions.size}", style = MaterialTheme.typography.bodySmall)
                        specialMatch?.let {
                            Text(
                                "پروفایل کاربر خاص: ${it.profile}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "انقضا: ${it.expiresAt?.let { ts -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(ts)) } ?: "بدون انقضا"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("مجوزهای Viewer", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("فایل سیاستی را که از برنامه مدیر دریافت کرده‌اید انتخاب کنید تا قابلیت‌های این Viewer فعال یا غیرفعال شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                var accessMessage by remember { mutableStateOf<String?>(null) }
                val accessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val result = onImportViewerAccess(uri)
                            accessMessage = result.fold({ it }, { "اعمال سیاست ناموفق بود: ${it.message ?: "خطای نامشخص"}" })
                        }
                    }
                }
                Button(onClick = { accessLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("دریافت سیاست دسترسی") }
                accessMessage?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text("پشتیبان‌گیری کامل", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "یادداشت‌ها، علاقه‌مندی‌ها، پاورقی‌ها، نقش‌های «من» و گفتگوهای شما در یک فایل ذخیره می‌شود. " +
                    "از پنجره‌ای که باز می‌شود می‌توانید محل ذخیره را انتخاب کنید: حافظه‌ی داخلی/کارت حافظه‌ی گوشی، " +
                    "یا اگر روی گوشی نصب باشد یک سرویس ابری مثل گوگل‌درایو. می‌توانید یک رمز هم روی فایل بگذارید " +
                    "(چون شامل یادداشت‌های شخصی شماست)؛ در این صورت فقط با همان رمز قابل بازیابی است — اگر رمز را " +
                    "فراموش کنید، هیچ راهی برای بازیابی آن فایل وجود ندارد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            var backupMessage by remember { mutableStateOf<String?>(null) }
            var pendingAction by remember { mutableStateOf<String?>(null) } // "backup" | "restore"
            var passwordInput by remember { mutableStateOf("") }
            var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

            val createBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        com.example.bookapp.data.writeBackupToUri(context, db, uri, passwordInput.ifBlank { null })
                        backupMessage = "پشتیبان با موفقیت ذخیره شد ✅"
                        passwordInput = ""
                    }
                }
            }
            val restoreBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    pendingRestoreUri = uri
                    pendingAction = "restore"
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pendingAction = "backup" }) {
                    Text("گرفتن پشتیبان")
                }
                OutlinedButton(onClick = {
                    restoreBackupLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
                }) {
                    Text("بازیابی از پشتیبان")
                }
            }
            backupMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            if (pendingAction == "backup") {
                AlertDialog(
                    onDismissRequest = { pendingAction = null; passwordInput = "" },
                    title = { Text("رمز عبور پشتیبان (اختیاری)") },
                    text = {
                        Column {
                            Text("اگر می‌خواهید فایل رمزگذاری شود، رمزی وارد کنید؛ برای بدون‌رمز خالی بگذارید.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("رمز (اختیاری)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingAction = null
                            createBackupLauncher.launch("پشتیبان-تعزیه.tazbackup")
                        }) { Text("ادامه") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null; passwordInput = "" }) { Text("انصراف") }
                    }
                )
            }

            if (pendingAction == "restore" && pendingRestoreUri != null) {
                AlertDialog(
                    onDismissRequest = { pendingAction = null; passwordInput = ""; pendingRestoreUri = null },
                    title = { Text("رمز عبور پشتیبان") },
                    text = {
                        Column {
                            Text("اگر این فایل هنگام ذخیره رمزگذاری شده، همان رمز را وارد کنید؛ اگر رمزی نداشت، خالی بگذارید.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("رمز (در صورت وجود)") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val uri = pendingRestoreUri!!
                            val pwd = passwordInput.ifBlank { null }
                            pendingAction = null; passwordInput = ""; pendingRestoreUri = null
                            scope.launch {
                                val result = com.example.bookapp.data.restoreBackupFromUri(context, db, uri, pwd)
                                backupMessage = if (result.isSuccess) {
                                    "بازیابی با موفقیت انجام شد ✅"
                                } else {
                                    "خطا در بازیابی: ${result.exceptionOrNull()?.message ?: "رمز اشتباه است یا فایل نامعتبر است"} ❌"
                                }
                            }
                        }) { Text("بازیابی") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingAction = null; passwordInput = ""; pendingRestoreUri = null }) { Text("انصراف") }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("گزارش خطا", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "اگر برنامه اخیراً برایتان کرش کرد (بسته شد)، جزئیاتش اینجا ذخیره شده؛ می‌توانید برای سازنده ارسال کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                val logFile = java.io.File(context.filesDir, "last_crash.txt")
                if (logFile.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "ارسال گزارش خطا"))
                } else {
                    syncMessage = "گزارش خطایی یافت نشد"
                }
            }) {
                Text("ارسال آخرین گزارش خطا")
            }
        }
    }
}

@Composable
private fun FontSizeOption(label: String, value: Float, current: Float, onSelect: (Float) -> Unit) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    FilterChip(
        selected = selected,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

@Composable
private fun LineSpacingOption(label: String, value: Float, current: Float, onSelect: (Float) -> Unit) {
    val selected = kotlin.math.abs(current - value) < 0.01f
    FilterChip(
        selected = selected,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    value: String,
    current: String,
    onSelect: (String) -> Unit,
    swatchColor: androidx.compose.ui.graphics.Color? = null
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        leadingIcon = swatchColor?.let {
            {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(it, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    )
}

@Composable
private fun ChangePasswordSection() {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val hasPasswordSet = Prefs.hasAppPassword(context)

    Column {
        if (hasPasswordSet) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("رمز فعلی") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("رمز جدید (برای غیرفعال‌کردن رمز، خالی بگذارید)") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("تکرار رمز جدید") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        message?.let {
            Text(
                it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = {
            val hasPassword = Prefs.hasAppPassword(context)
            when {
                hasPassword && !Prefs.verifyAppPassword(context, currentPassword) -> {
                    isError = true
                    message = "رمز فعلی درست نیست"
                }
                newPassword != confirmPassword -> {
                    isError = true
                    message = "رمز جدید و تکرار آن یکسان نیستند"
                }
                else -> {
                    Prefs.setAppPassword(context, newPassword)
                    isError = false
                    message = if (newPassword.isBlank()) "رمز عبور غیرفعال شد" else "رمز عبور با موفقیت تغییر کرد ✅"
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                }
            }
        }) {
            Text("ذخیره رمز")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ورژن برنامه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("نسخه برنامه: ${BuildConfig.VERSION_NAME}")
        }
    }
}
