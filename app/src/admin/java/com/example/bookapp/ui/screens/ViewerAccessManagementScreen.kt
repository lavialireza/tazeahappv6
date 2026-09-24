package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookapp.data.ViewerAccessPolicy
import com.example.bookapp.data.ViewerAccessTransfer
import com.example.bookapp.data.AccessAuditLog
import com.example.bookapp.data.PermissionQa
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ClipData
import androidx.compose.material3.ExperimentalMaterial3Api


private data class PermissionTree(
    val categoryTitle: String,
    val children: List<String>
)

/**
 * درخت سه‌سطحی دسترسی:
 * دسته‌بندی ← قابلیت والد ← عملیات/قابلیت فرزند.
 * دسته‌بندی کلید مستقل سیاست نیست و فقط برای سازمان‌دهی است؛ هر قابلیت والد
 * و هر فرزند کلید مستقل خود را در ViewerAccessPolicy دارد.
 */
private val accessPermissionTrees = listOf(
    PermissionTree("مطالعه و جستجو", listOf("read", "search", "advancedSearch", "compare", "training")),
    PermissionTree("رسانه و نمایش", listOf("audio", "tts", "gallery")),
    PermissionTree("امکانات شخصی", listOf("notes", "bookmarks", "copy", "share")),
    PermissionTree("امکانات پژوهشی", listOf("footnotes", "dictionary", "taziehCorrections")),
    PermissionTree("نقش و تمرین", listOf("myRole")),
    PermissionTree("خروجی", listOf("pdf")),
    PermissionTree("تقویم و اطلاعات", listOf("calendar", "appIntro"))
)

private val permissionChildren = mapOf(
    "read" to listOf("read.view", "read.navigate"),
    "search" to listOf("search.basic"),
    "advancedSearch" to listOf("advancedSearch.filters"),
    "compare" to listOf("compare.view"),
    "training" to listOf("training.rehearse"),
    "audio" to listOf("audio.play"),
    "tts" to listOf("tts.play"),
    "notes" to listOf("notes.view", "notes.add", "notes.edit", "notes.delete"),
    "bookmarks" to listOf("bookmarks.view", "bookmarks.add", "bookmarks.delete"),
    "gallery" to listOf("gallery.view", "gallery.add", "gallery.edit", "gallery.delete", "gallery.largePreview"),
    "copy" to listOf("copy.text"),
    "share" to listOf("share.content"),
    "pdf" to listOf("pdf.create", "pdf.save"),
    "footnotes" to listOf("footnotes.view", "footnotes.add", "footnotes.edit", "footnotes.delete", "footnoteSync.dictionary"),
    "appIntro" to listOf("appIntro.view"),
    "myRole" to listOf("myRole.view", "myRole.select", "myRole.remove", "myRole.rehearse", "myRole.pdf"),
    "dictionary" to listOf("dictionary.view", "dictionary.add", "dictionary.edit", "dictionary.delete"),
    "taziehCorrections" to listOf("taziehCorrections.view", "taziehCorrections.add", "taziehCorrections.edit", "taziehCorrections.delete", "taziehCorrections.apply"),
    "calendar" to listOf("calendar.view", "calendar.suggestions")
)

private fun permissionDescription(key: String): String = when (key) {
    "read" -> "والد مطالعه؛ کنترل کلی دسترسی به مطالعه"
    "search" -> "والد جستجوی معمولی"
    "advancedSearch" -> "والد جستجوی پیشرفته"
    "compare" -> "والد مقایسه متون"
    "training" -> "والد تمرین و بازخوانی"
    "audio" -> "والد امکانات صوتی"
    "tts" -> "والد تبدیل متن به گفتار"
    "notes" -> "والد یادداشت‌های شخصی"
    "bookmarks" -> "والد علاقه‌مندی‌ها"
    "gallery" -> "والد گالری"
    "copy" -> "والد کپی متن"
    "share" -> "والد اشتراک‌گذاری"
    "pdf" -> "والد خروجی PDF"
    "footnotes" -> "والد پاورقی؛ فرزندان آن مشاهده، افزودن، ویرایش، حذف و همگام‌سازی دیکشنری هستند"
    "appIntro" -> "والد معرفی برنامه"
    "myRole" -> "والد نقش من؛ مدیریت نقش‌های انتخاب‌شده و عملیات مرتبط"
    "dictionary" -> "والد دیکشنری اصطلاحات تعزیه؛ فرزندان: مشاهده، افزودن، ویرایش، حذف"
    "taziehCorrections" -> "والد دیکشنری اصلاحات تعزیه؛ مستقل از دیکشنری اصطلاحات"
    "calendar" -> "والد تقویم محرم و پیشنهادهای مرتبط"
    else -> "قابلیت جزئی مستقل"
}

private val sensitivePermissionKeys = setOf("copy", "share", "pdf", "gallery", "training", "footnotes.delete", "notes.delete", "bookmarks.delete", "myRole.remove", "dictionary.delete", "taziehCorrections.delete")

private fun accessDateTime(value: Long): String =
    if (value <= 0L) "ثبت نشده"
    else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(value))

@Composable
private fun PermissionGroupEditor(
    permissions: Map<String, Boolean>,
    onChange: (String, Boolean) -> Unit,
    onBulkChange: (List<String>, Boolean) -> Unit,
    searchQuery: String,
    compact: Boolean = false
) {
    var expandedCategories by remember { mutableStateOf(setOf("امکانات پژوهشی")) }
    var expandedParents by remember { mutableStateOf(setOf("dictionary")) }
    val labels = ViewerAccessPolicy.permissionLabels

    val visible = accessPermissionTrees.mapNotNull { category ->
        val parentMatches = category.children.filter { key ->
            labels[key]?.contains(searchQuery, ignoreCase = true) == true || key.contains(searchQuery, ignoreCase = true)
        }
        val matchingChildren = category.children.flatMap { parent ->
            permissionChildren[parent].orEmpty().filter { key ->
                labels[key]?.contains(searchQuery, ignoreCase = true) == true || key.contains(searchQuery, ignoreCase = true)
            }
        }
        if (searchQuery.isBlank() || parentMatches.isNotEmpty() || matchingChildren.isNotEmpty()) {
            category to if (searchQuery.isBlank()) category.children else parentMatches + matchingChildren.mapNotNull { child -> ViewerAccessPolicy.permissionParents[child] }.filterNot { it in parentMatches }.distinct()
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visible.forEach { (category, _) ->
            val parentKeys = category.children
            val allParents = parentKeys.all { permissions[it] == true }
            val activeParents = parentKeys.count { permissions[it] == true }
            val categoryExpanded = category.categoryTitle in expandedCategories
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).clickable {
                            expandedCategories = if (categoryExpanded) expandedCategories - category.categoryTitle else expandedCategories + category.categoryTitle
                        }) {
                            Text("گروه: ${category.categoryTitle}", style = MaterialTheme.typography.titleSmall)
                            Text("قابلیت‌های اصلی فعال: $activeParents از ${parentKeys.size}", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = allParents,
                            onCheckedChange = { value -> onBulkChange(parentKeys + parentKeys.flatMap { permissionChildren[it].orEmpty() }, value) }
                        )
                    }
                    if (categoryExpanded) {
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { onBulkChange(parentKeys + parentKeys.flatMap { permissionChildren[it].orEmpty() }, true) }) { Text("فعال کردن والدها") }
                            TextButton(onClick = { onBulkChange(parentKeys + parentKeys.flatMap { permissionChildren[it].orEmpty() }, false) }) { Text("غیرفعال کردن والدها") }
                        }
                        parentKeys.forEach { parentKey ->
                            val parentEnabled = permissions[parentKey] == true
                            val childKeys = permissionChildren[parentKey].orEmpty()
                            val childEnabledCount = childKeys.count { permissions[it] == true }
                            val allChildren = childKeys.isNotEmpty() && childKeys.all { permissions[it] == true }
                            val parentExpanded = parentKey in expandedParents || searchQuery.isNotBlank()
                            Card(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp)) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Column(Modifier.weight(1f).clickable {
                                            expandedParents = if (parentExpanded) expandedParents - parentKey else expandedParents + parentKey
                                        }) {
                                            Text("والد: ${labels[parentKey] ?: parentKey}", style = MaterialTheme.typography.bodyLarge)
                                            Text("فرزندان فعال: $childEnabledCount از ${childKeys.size}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Switch(
                                            checked = parentEnabled,
                                            onCheckedChange = { value ->
                                                onChange(parentKey, value)
                                                if (!value && childKeys.isNotEmpty()) onBulkChange(childKeys, false)
                                                if (value && childEnabledCount == 0 && childKeys.isNotEmpty()) onBulkChange(childKeys, true)
                                            }
                                        )
                                    }
                                    if (parentExpanded) {
                                        Text(permissionDescription(parentKey), style = MaterialTheme.typography.bodySmall)
                                        if (childKeys.isNotEmpty()) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                TextButton(enabled = parentEnabled, onClick = { onBulkChange(childKeys, true) }) { Text("فعال کردن فرزندان") }
                                                TextButton(enabled = parentEnabled, onClick = { onBulkChange(childKeys, false) }) { Text("غیرفعال کردن فرزندان") }
                                            }
                                            childKeys.forEach { key ->
                                                val label = labels[key] ?: key
                                                Row(Modifier.fillMaxWidth().padding(start = 18.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text("فرزند: $label")
                                                        Text(permissionDescription(key), style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    Switch(enabled = parentEnabled, checked = permissions[key] == true, onCheckedChange = { value -> onChange(key, value) })
                                                }
                                            }
                                            if (allChildren) Text("همه فرزندان فعال هستند.", style = MaterialTheme.typography.labelSmall)
                                            else if (childEnabledCount > 0) Text("والد فعال است و فقط بخشی از فرزندان فعال هستند.", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ViewerAccessManagementScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var publicPermissions by remember { mutableStateOf(ViewerAccessPolicy.getPublicPermissions(context)) }
    var specialUsers by remember { mutableStateOf(ViewerAccessPolicy.getSpecialUsers(context)) }
    var selectedUser by remember { mutableStateOf<ViewerAccessPolicy.SpecialUser?>(null) }
    var installationId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf(ViewerAccessPolicy.PROFILE_CUSTOM) }
    var expiryText by remember { mutableStateOf("") }
    var customPermissions by remember { mutableStateOf(ViewerAccessPolicy.profileDefaults(profile)) }
    var message by remember { mutableStateOf<String?>(null) }
    var accessTestMessage by remember { mutableStateOf<String?>(null) }
    var exportTarget by remember { mutableStateOf("*") }
    var permissionSearch by remember { mutableStateOf("") }
    var savedPublicPermissions by remember { mutableStateOf(publicPermissions) }
    var savedUserPermissions by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    var pendingPermissionChange by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var pendingPermissionContext by remember { mutableStateOf("") }
    var pendingPermissionForUser by remember { mutableStateOf(false) }
    var pendingBulkChange by remember { mutableStateOf<Pair<List<String>, Boolean>?>(null) }
    var pendingBulkForUser by remember { mutableStateOf(false) }
    var lastPublicChangeAt by remember { mutableStateOf(0L) }
    var showPermissionPreview by remember { mutableStateOf(false) }
    var showAccessHistory by remember { mutableStateOf(false) }
    var qaResult by remember { mutableStateOf<PermissionQa.Result?>(null) }
    var showComparePolicies by remember { mutableStateOf(false) }
    var showSettingsImportConfirm by remember { mutableStateOf(false) }
    var pendingSettingsJson by remember { mutableStateOf<String?>(null) }
    var showDraftTest by remember { mutableStateOf(false) }
    val publicDirty = publicPermissions != savedPublicPermissions
    val userDirty = selectedUser != null && customPermissions != (savedUserPermissions ?: selectedUser!!.permissions)
    fun requestPermissionChange(key: String, value: Boolean, forUser: Boolean) {
        if (!value && key in sensitivePermissionKeys) {
            pendingPermissionChange = key to value
            pendingPermissionContext = ViewerAccessPolicy.permissionLabels[key] ?: key
            pendingPermissionForUser = forUser
        } else if (forUser) {
            customPermissions = customPermissions + (key to value)
        } else {
            publicPermissions = publicPermissions + (key to value)
        }
    }
    fun requestBulkChange(keys: List<String>, value: Boolean, forUser: Boolean) {
        if (keys.isEmpty()) return
        if (!value && keys.any { it in sensitivePermissionKeys }) {
            pendingBulkChange = keys to value
            pendingBulkForUser = forUser
            pendingPermissionContext = keys.mapNotNull { ViewerAccessPolicy.permissionLabels[it] }.joinToString("، ")
        } else {
            val target = if (forUser) customPermissions else publicPermissions
            val updated = target.toMutableMap()
            keys.forEach { updated[it] = value }
            if (forUser) customPermissions = updated else publicPermissions = updated
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.let { ViewerAccessTransfer.writePolicy(context, exportTarget, it) } ?: error("فایل خروجی باز نشد.") }
                .onSuccess { message = "فایل سیاست دسترسی آماده شد؛ آن را به Viewer منتقل کنید." }
                .onFailure { message = "خروجی سیاست ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
        }
    }
    val settingsExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(ViewerAccessPolicy.exportSettingsJson(context).toByteArray(Charsets.UTF_8)) }
                ?: error("فایل خروجی باز نشد.")
        }.onSuccess { message = "پشتیبان کامل تنظیمات دسترسی ذخیره شد." }
         .onFailure { message = "خروجی تنظیمات ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
    }
    val settingsImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("فایل خوانده نشد.")
        }.onSuccess { pendingSettingsJson = it; showSettingsImportConfirm = true }
         .onFailure { message = "خواندن تنظیمات ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
    }

    fun sendDirectlyToViewer(target: String) {
        fun markSentIfSpecial() {
            val normalized = target.trim().uppercase(Locale.US)
            if (normalized.isNotBlank() && normalized != "*") {
                val version = ViewerAccessPolicy.getPolicyVersion(context, normalized)
                ViewerAccessPolicy.markPolicySent(context, normalized, version)
                specialUsers = ViewerAccessPolicy.getSpecialUsers(context)
            }
        }
        runCatching {
            val uri = ViewerAccessTransfer.createShareUri(context, target)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.example.bookapp.viewer")
            }
            context.startActivity(intent)
            markSentIfSpecial()
            val version = ViewerAccessPolicy.getPolicyVersion(context, target)
            context.sendBroadcast(Intent("com.example.bookapp.POLICY_CHANGED").apply {
                setPackage("com.example.bookapp.viewer")
                putExtra("targetInstallationId", target.trim().uppercase(Locale.US))
                putExtra("policyVersion", version)
            })
            message = "سیاست نسخه $version ارسال شد؛ Viewer برای اطلاع از تغییر سیاست به‌روزرسانی شد و تأیید اعمال جداگانه دریافت می‌شود."
        }.onFailure {
            // اگر Viewer نصب نیست، همان فایل را از طریق Share Sheet در اختیار کاربر می‌گذاریم.
            runCatching {
                val uri = ViewerAccessTransfer.createShareUri(context, target)
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallback, "ارسال سیاست دسترسی به Viewer"))
            }.onFailure { e -> message = "ارسال به Viewer ناموفق بود: ${e.message ?: "خطای نامشخص"}" }
        }
    }

    fun loadUser(user: ViewerAccessPolicy.SpecialUser) {
        selectedUser = user
        installationId = user.installationId
        displayName = user.displayName
        details = user.details
        enabled = user.enabled
        profile = user.profile
        expiryText = user.expiresAt?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it)) } ?: ""
        customPermissions = user.permissions
        savedUserPermissions = user.permissions
    }
    fun resetEditor() {
        selectedUser = null; installationId = ""; displayName = ""; details = ""; enabled = true; profile = ViewerAccessPolicy.PROFILE_CUSTOM; expiryText = ""; customPermissions = ViewerAccessPolicy.profileDefaults(ViewerAccessPolicy.PROFILE_CUSTOM); savedUserPermissions = null
    }

    pendingPermissionChange?.let { (key, value) ->
        AlertDialog(
            onDismissRequest = { pendingPermissionChange = null; pendingPermissionForUser = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("تغییر دسترسی حساس") },
            text = { Text("آیا مطمئن هستید دسترسی «$pendingPermissionContext» غیرفعال شود؟ پس از ذخیره و همگام‌سازی، Viewer دیگر این قابلیت را نخواهد داشت.") },
            confirmButton = {
                TextButton(onClick = {
                    if (pendingPermissionForUser) customPermissions = customPermissions + (key to value)
                    else publicPermissions = publicPermissions + (key to value)
                    pendingPermissionChange = null
                    pendingPermissionForUser = false
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { pendingPermissionChange = null }) { Text("انصراف") } }
        )
    }

    pendingBulkChange?.let { (keys, value) ->
        AlertDialog(
            onDismissRequest = { pendingBulkChange = null; pendingBulkForUser = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("تغییر گروهی دسترسی‌ها") },
            text = { Text("آیا مطمئن هستید ${if (value) "همه" else "دسترسی‌های انتخاب‌شده در گروه"} اعمال شود؟ این تغییر شامل قابلیت‌های حساس نیز می‌شود: $pendingPermissionContext") },
            confirmButton = {
                TextButton(onClick = {
                    val target = if (pendingBulkForUser) customPermissions else publicPermissions
                    val updated = target.toMutableMap()
                    keys.forEach { updated[it] = value }
                    if (pendingBulkForUser) customPermissions = updated else publicPermissions = updated
                    pendingBulkChange = null
                    pendingBulkForUser = false
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { pendingBulkChange = null; pendingBulkForUser = false }) { Text("انصراف") } }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("مدیریت دسترسی Viewer") }, navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("پروفایل عمومی", style = MaterialTheme.typography.titleMedium)
                        Text("این پروفایل برای همه کاربران عمومی است و تغییرات آن در انتشار بروزرسانی بعدی Viewer اعمال می‌شود.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = permissionSearch,
                            onValueChange = { permissionSearch = it },
                            label = { Text("جستجوی دسترسی") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        PermissionGroupEditor(
                            permissions = publicPermissions,
                            onChange = { key, value -> requestPermissionChange(key, value, false) },
                            onBulkChange = { keys, value -> requestBulkChange(keys, value, false) },
                            searchQuery = permissionSearch
                        )
                        Button(onClick = { ViewerAccessPolicy.setPublicPermissions(context, publicPermissions); savedPublicPermissions = publicPermissions; lastPublicChangeAt = System.currentTimeMillis(); message = "پروفایل عمومی ذخیره شد و آماده ارسال است." }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("ذخیره پروفایل عمومی") }
                        if (publicDirty) {
                            Text("تغییرات ذخیره‌نشده: ${publicPermissions.keys.count { publicPermissions[it] != savedPublicPermissions[it] }} مورد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Text("آخرین تغییر این صفحه: ${if (lastPublicChangeAt > 0L) accessDateTime(lastPublicChangeAt) else "ثبت نشده"}", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { exportTarget = "*"; exportLauncher.launch("viewer-access-public.json") }, modifier = Modifier.fillMaxWidth()) { Text("خروجی سیاست عمومی برای Viewer") }
                        Button(onClick = { sendDirectlyToViewer("*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(6.dp)); Text("ارسال مستقیم به Viewer") }
                        Text("نسخه سیاست: ${ViewerAccessPolicy.getPolicyVersion(context)}", style = MaterialTheme.typography.bodySmall)
                        Text(if (publicDirty) "وضعیت: ⚠ تغییرات ذخیره‌نشده" else "وضعیت: ✓ تنظیمات ذخیره‌شده", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (selectedUser == null) "افزودن کاربر خاص" else "ویرایش کاربر خاص", style = MaterialTheme.typography.titleMedium)
                        Text("برای هر Viewer یک پرونده مدیریتی مستقل نگه‌داری می‌شود. شناسه نصب شماره تلفن یا IMEI نیست.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(displayName, { displayName = it }, label = { Text("نام کاربر") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(details, { details = it }, label = { Text("مشخصات / توضیحات کاربر") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(if (enabled) "وضعیت: فعال" else "وضعیت: غیرفعال", style = MaterialTheme.typography.bodyMedium)
                                Text(if (enabled) "دسترسی اختصاصی این کاربر قابل اعمال است." else "دسترسی اختصاصی این کاربر فعلاً اعمال نمی‌شود.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        OutlinedTextField(installationId, { installationId = it.uppercase(Locale.US) }, label = { Text("شناسه نصب") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Text("پروفایل آماده", style = MaterialTheme.typography.titleSmall)
                        Text("با انتخاب یک پروفایل، مجوزهای پیشنهادی آن اعمال می‌شود و در صورت نیاز قابل سفارشی‌سازی است.", style = MaterialTheme.typography.bodySmall)
                        val profileRows = listOf(
                            listOf(ViewerAccessPolicy.PROFILE_PUBLIC to "عمومی", ViewerAccessPolicy.PROFILE_RESEARCHER to "پژوهشگر", ViewerAccessPolicy.PROFILE_DIRECTOR to "کارگردان", ViewerAccessPolicy.PROFILE_ACTOR to "بازیگر"),
                            listOf(ViewerAccessPolicy.PROFILE_READER to "خواننده", ViewerAccessPolicy.PROFILE_TRAINING to "تمرینی", ViewerAccessPolicy.PROFILE_COLLABORATOR to "همکار", ViewerAccessPolicy.PROFILE_CUSTOM to "سفارشی")
                        )
                        profileRows.forEach { rowProfiles ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                rowProfiles.forEach { (value, label) ->
                                    FilterChip(
                                        selected = profile == value,
                                        onClick = { profile = value; customPermissions = ViewerAccessPolicy.profileDefaults(value) },
                                        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    )
                                }
                            }
                        }
                        OutlinedTextField(expiryText, { expiryText = it }, label = { Text("انقضا (YYYY-MM-DD، خالی = بدون انقضا)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        if (profile == ViewerAccessPolicy.PROFILE_CUSTOM) {
                            Text("مجوزهای اختصاصی", style = MaterialTheme.typography.titleSmall)
                            PermissionGroupEditor(
                                permissions = customPermissions,
                                onChange = { key, value -> requestPermissionChange(key, value, true) },
                                onBulkChange = { keys, value -> requestBulkChange(keys, value, true) },
                                searchQuery = permissionSearch,
                                compact = true
                            )
                        }
                        if (profile == ViewerAccessPolicy.PROFILE_CUSTOM) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "خلاصه مجوزها: ${customPermissions.count { it.value }} از ${ViewerAccessPolicy.permissionLabels.size} قابلیت فعال",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (userDirty) Text("⚠ تغییرات مجوز ذخیره نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        selectedUser?.let { u ->
                            Spacer(Modifier.height(4.dp))
                            Text("آخرین تغییر: ${accessDateTime(u.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (ViewerAccessPolicy.policyNeedsResend(u)) "وضعیت سیاست: ⚠ نیاز به همگام‌سازی"
                                else if (u.lastPolicyAppliedAt > 0L && u.lastPolicyAppliedVersion == u.lastPolicySentVersion) "وضعیت سیاست: ✓ همگام"
                                else "وضعیت سیاست: ارسال شده، در انتظار تأیید Viewer",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("آخرین ارسال: ${accessDateTime(u.lastPolicySentAt)} | آخرین اعمال: ${accessDateTime(u.lastPolicyAppliedAt)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val normalizedId = installationId.trim().uppercase(Locale.US)
                                if (normalizedId.isBlank()) { message = "شناسه نصب را وارد کنید."; return@Button }
                                val expiry = expiryText.trim().takeIf { it.isNotBlank() }?.let { raw -> runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(raw)?.time }.getOrNull() }
                                if (expiryText.isNotBlank() && expiry == null) { message = "تاریخ انقضا معتبر نیست."; return@Button }
                                runCatching {
                                    ViewerAccessPolicy.upsertSpecialUser(
                                        context,
                                        ViewerAccessPolicy.SpecialUser(
                                            installationId = normalizedId,
                                            profile = profile,
                                            expiresAt = expiry,
                                            permissions = customPermissions,
                                            displayName = displayName.trim(),
                                            details = details.trim(),
                                            phone = selectedUser?.phone ?: "",
                                            address = selectedUser?.address ?: "",
                                            position = selectedUser?.position ?: "",
                                            userType = selectedUser?.userType ?: "",
                                            otherDetails = selectedUser?.otherDetails ?: "",
                                            enabled = enabled,
                                            createdAt = selectedUser?.createdAt ?: System.currentTimeMillis()
                                        )
                                    )
                                    val reloaded = ViewerAccessPolicy.getSpecialUsers(context)
                                    check(reloaded.any { it.installationId.equals(normalizedId, ignoreCase = true) }) { "شناسه پس از ذخیره پیدا نشد." }
                                    specialUsers = reloaded
                                    // فرم را پاک نمی‌کنیم تا کاربر بلافاصله اطلاعات ذخیره‌شده را ببیند
                                    // و بتواند در صورت نیاز همان رکورد را دوباره ویرایش کند.
                                    selectedUser = reloaded.firstOrNull { it.installationId.equals(normalizedId, ignoreCase = true) }
                                    savedUserPermissions = customPermissions
                                    installationId = normalizedId
                                    message = "کاربر خاص «$normalizedId» واقعاً در حافظه برنامه ذخیره و بازیابی شد."
                                }.onFailure { e ->
                                    message = "ذخیره کاربر خاص انجام نشد: ${e.message ?: "خطای نامشخص"}"
                                }
                            }, Modifier.weight(1f)) { Text("ذخیره و اعمال") }
                            OutlinedButton(onClick = { resetEditor(); accessTestMessage = null }, Modifier.weight(1f)) { Text("جدید") }
                        }
                        if (installationId.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val id = installationId.trim().uppercase(Locale.US)
                                    val match = ViewerAccessPolicy.getSpecialUsers(context).firstOrNull { it.installationId == id }
                                    accessTestMessage = if (match == null) {
                                        "نتیجه آزمون: شناسه «$id» در فهرست کاربران خاص ذخیره نشده است."
                                    } else {
                                        val expired = match.expiresAt != null && match.expiresAt > 0L && System.currentTimeMillis() > match.expiresAt
                                        val active = match.permissions.count { it.value }
                                        if (!match.enabled) "نتیجه آزمون: «${match.displayName.ifBlank { id }}» غیرفعال است."
                                        else if (expired) "نتیجه آزمون: «${match.displayName.ifBlank { id }}» پیدا شد، اما دسترسی آن منقضی شده است."
                                        else "نتیجه آزمون: «${match.displayName.ifBlank { id }}» فعال است؛ $active قابلیت فعال دارد."
                                    }
                                }, Modifier.weight(1f)) { Text("آزمون دسترسی") }
                                OutlinedButton(onClick = {
                                    val id = installationId.trim().uppercase(Locale.US)
                                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("شناسه نصب Viewer", id))
                                    message = "شناسه «$id» در کلیپ‌بورد کپی شد."
                                }, Modifier.weight(1f)) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("کپی شناسه") }
                            }
                            accessTestMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                        }
                        if (installationId.isNotBlank() && specialUsers.any { it.installationId.equals(installationId.trim(), ignoreCase = true) }) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { exportTarget = installationId.trim(); exportLauncher.launch("viewer-access-${installationId.trim()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("خروجی سیاست این Viewer") }
                            Button(onClick = { sendDirectlyToViewer(installationId.trim()) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(6.dp)); Text("ارسال مستقیم به همین Viewer") }
                            if (userDirty) {
                                OutlinedButton(onClick = {
                                    val id = installationId.trim().uppercase(Locale.US)
                                    val current = specialUsers.firstOrNull { it.installationId.equals(id, ignoreCase = true) }
                                    if (current != null) {
                                        ViewerAccessPolicy.upsertSpecialUser(context, current.copy(permissions = customPermissions, profile = profile, enabled = enabled))
                                        specialUsers = ViewerAccessPolicy.getSpecialUsers(context)
                                        val updated = ViewerAccessPolicy.getSpecialUsers(context).firstOrNull { it.installationId == id }
                                        savedUserPermissions = customPermissions
                                        if (updated != null) {
                                            sendDirectlyToViewer(id)
                                            message = "تغییرات اعمال و برای Viewer ارسال شد."
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(6.dp)); Text("اعمال تغییرات و ارسال") }
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ابزارهای کنترل دسترسی", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { showPermissionPreview = true }, Modifier.weight(1f)) { Text("پیش‌نمایش") }
                            OutlinedButton(enabled = selectedUser != null, onClick = { showComparePolicies = true }, modifier = Modifier.weight(1f)) { Text("مقایسه سیاست") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { showDraftTest = true }, Modifier.weight(1f)) { Text("آزمون پیش از اعمال") }
                            OutlinedButton(onClick = { settingsExportLauncher.launch("tazieh-access-settings.json") }, Modifier.weight(1f)) { Text("خروجی تنظیمات") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { settingsImportLauncher.launch(arrayOf("application/json", "text/*")) }, Modifier.weight(1f)) { Text("ورود تنظیمات") }
                            OutlinedButton(onClick = { permissionSearch = "" }, Modifier.weight(1f)) { Text("پاک‌کردن جستجو") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { showAccessHistory = true }, Modifier.weight(1f)) { Text("تاریخچه تغییرات") }
                            OutlinedButton(onClick = { qaResult = PermissionQa.run(context) }, Modifier.weight(1f)) { Text("آزمون ۶۵ مجوز") }
                        }
                        qaResult?.let { r ->
                            Text(if (r.failed == 0) "✓ آزمون موفق: ${r.passed} بررسی بدون خطا" else "⚠ ${r.failed} مورد نیازمند بررسی؛ ${r.passed} بررسی موفق", color = if (r.failed == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            if (r.failures.isNotEmpty()) r.failures.take(8).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("کاربران خاص (${specialUsers.size})", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        specialUsers = ViewerAccessPolicy.getSpecialUsers(context)
                        message = "فهرست کاربران خاص از حافظه پایدار دوباره بارگذاری شد."
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "بارگذاری مجدد")
                    }
                }
            }
            items(specialUsers, key = { it.installationId }) { user ->
                Card(Modifier.fillMaxWidth().clickable { loadUser(user) }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(user.displayName.ifBlank { "بدون نام" }, style = MaterialTheme.typography.titleSmall)
                            Text("شناسه: ${user.installationId}", style = MaterialTheme.typography.bodySmall)
                            Text("پروفایل: ${profileTitle(user.profile)} | ${if (user.enabled) "فعال" else "غیرفعال"}", style = MaterialTheme.typography.bodySmall)
                            Text("انقضا: ${user.expiresAt?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it)) } ?: "بدون انقضا"}", style = MaterialTheme.typography.bodySmall)
                            if (user.details.isNotBlank()) Text(user.details, style = MaterialTheme.typography.bodySmall)
                            Text("مجوزهای فعال: ${user.permissions.count { it.value }} از ${ViewerAccessPolicy.permissionLabels.size}", style = MaterialTheme.typography.bodySmall)
                            Text("آخرین تغییر: ${accessDateTime(user.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (ViewerAccessPolicy.policyNeedsResend(user)) "⚠ نیاز به همگام‌سازی"
                                else if (user.lastPolicyAppliedAt > 0L && user.lastPolicyAppliedVersion == user.lastPolicySentVersion) "✓ همگام با Viewer"
                                else "ارسال شده؛ در انتظار اعمال Viewer",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { loadUser(user) }) { Text("ویرایش") }
                        IconButton(onClick = { ViewerAccessPolicy.removeSpecialUser(context, user.installationId); specialUsers = ViewerAccessPolicy.getSpecialUsers(context) }) { Icon(Icons.Filled.Delete, null) }
                    }
                }
            }
            item { Text("شناسه نصب این دستگاه: ${ViewerAccessPolicy.installationId(context)}", style = MaterialTheme.typography.bodySmall) }
            if (message != null) item { Text(message!!, color = MaterialTheme.colorScheme.primary) }
        }
    }

    if (showPermissionPreview) {
        val target = if (selectedUser != null) customPermissions else publicPermissions
        AlertDialog(
            onDismissRequest = { showPermissionPreview = false },
            title = { Text("پیش‌نمایش دسترسی ${if (selectedUser != null) "Viewer انتخاب‌شده" else "عمومی"}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Text("فعال: ${target.count { it.value }} از ${ViewerAccessPolicy.permissionLabels.size}") }
                    items(ViewerAccessPolicy.permissionLabels.toList()) { (key, label) ->
                        val effective = target[key] == true && (ViewerAccessPolicy.permissionParents[key]?.let { target[it] == true } ?: true)
                        Text("${if (effective) "✓" else "✗"} $label", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPermissionPreview = false }) { Text("بستن") } }
        )
    }

    if (showAccessHistory) {
        val history = AccessAuditLog.getAll(context).take(30)
        AlertDialog(
            onDismissRequest = { showAccessHistory = false },
            title = { Text("تاریخچه تغییرات دسترسی") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (history.isEmpty()) item { Text("هنوز سابقه‌ای ثبت نشده است.") }
                    items(history) { e ->
                        Column {
                            Text("${accessDateTime(e.timestamp)} — ${e.action}", style = MaterialTheme.typography.bodyMedium)
                            if (e.displayName.isNotBlank()) Text(e.displayName, style = MaterialTheme.typography.bodySmall)
                            if (e.details.isNotBlank()) Text(e.details, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAccessHistory = false }) { Text("بستن") } }
        )
    }

    if (showComparePolicies && selectedUser != null) {
        val diffs = ViewerAccessPolicy.permissionLabels.keys.mapNotNull { key ->
            val publicValue = publicPermissions[key] == true
            val userValue = customPermissions[key] == true
            if (publicValue != userValue) ViewerAccessPolicy.permissionLabels[key] to Pair(publicValue, userValue) else null
        }
        AlertDialog(
            onDismissRequest = { showComparePolicies = false },
            title = { Text("مقایسه سیاست عمومی و کاربر") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item { Text("تفاوت‌ها: ${diffs.size} مورد", style = MaterialTheme.typography.titleSmall) }
                    if (diffs.isEmpty()) item { Text("هیچ تفاوتی در مجوزهای این پیش‌نویس وجود ندارد.") }
                    items(diffs) { (label, values) ->
                        Text("$label: عمومی ${if (values.first) "روشن" else "خاموش"} / کاربر ${if (values.second) "روشن" else "خاموش"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showComparePolicies = false }) { Text("بستن") } }
        )
    }

    if (showDraftTest) {
        val draft = if (selectedUser != null) customPermissions else publicPermissions
        val effective = ViewerAccessPolicy.normalizedPermissions(draft)
        val inactiveChildren = ViewerAccessPolicy.permissionParents.count { (child, parent) -> effective[child] == true && effective[parent] != true }
        AlertDialog(
            onDismissRequest = { showDraftTest = false },
            title = { Text("آزمون پیش از اعمال") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("این آزمون هیچ تغییری را ذخیره نمی‌کند.")
                    Text("مجوزهای روشن: ${effective.count { it.value }} از ${ViewerAccessPolicy.permissionLabels.size}")
                    Text(if (inactiveChildren == 0) "✓ رابطه والد/فرزند بدون تناقض است." else "⚠ $inactiveChildren فرزند به‌دلیل خاموش بودن والد مؤثر نیست.")
                    Text("پس از ذخیره، برای اعمال روی Viewer باید سیاست ارسال شود.")
                }
            },
            confirmButton = { TextButton(onClick = { qaResult = PermissionQa.run(context); showDraftTest = false }) { Text("آزمون کامل") } },
            dismissButton = { TextButton(onClick = { showDraftTest = false }) { Text("بستن") } }
        )
    }

    if (showSettingsImportConfirm && pendingSettingsJson != null) {
        AlertDialog(
            onDismissRequest = { showSettingsImportConfirm = false; pendingSettingsJson = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("جایگزینی تنظیمات دسترسی") },
            text = { Text("پروفایل عمومی و فهرست کاربران خاص موجود با اطلاعات این فایل جایگزین می‌شوند. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    ViewerAccessPolicy.importSettingsJson(context, pendingSettingsJson!!)
                        .onSuccess { message = it; publicPermissions = ViewerAccessPolicy.getPublicPermissions(context); specialUsers = ViewerAccessPolicy.getSpecialUsers(context); savedPublicPermissions = publicPermissions }
                        .onFailure { message = "ورود تنظیمات ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
                    showSettingsImportConfirm = false; pendingSettingsJson = null
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { showSettingsImportConfirm = false; pendingSettingsJson = null }) { Text("انصراف") } }
        )
    }
}

private fun profileTitle(profile: String) = when (profile) {
    ViewerAccessPolicy.PROFILE_PUBLIC -> "عمومی"
    ViewerAccessPolicy.PROFILE_TRAINING -> "تمرینی"
    ViewerAccessPolicy.PROFILE_COLLABORATOR -> "همکار"
    ViewerAccessPolicy.PROFILE_RESEARCHER -> "پژوهشگر"
    ViewerAccessPolicy.PROFILE_DIRECTOR -> "کارگردان"
    ViewerAccessPolicy.PROFILE_ACTOR -> "بازیگر"
    ViewerAccessPolicy.PROFILE_READER -> "خواننده"
    else -> "سفارشی"
}
