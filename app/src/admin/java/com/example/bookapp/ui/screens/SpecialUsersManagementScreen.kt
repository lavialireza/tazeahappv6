package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ContentCopy
import android.content.Intent
import android.net.Uri
import com.example.bookapp.data.ViewerAccessTransfer
import com.example.bookapp.data.AccessAuditLog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.ViewerAccessPolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialUsersManagementScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var users by remember { mutableStateOf(ViewerAccessPolicy.getSpecialUsers(context)) }
    var selected by remember { mutableStateOf<ViewerAccessPolicy.SpecialUser?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var sortMode by remember { mutableStateOf("name") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    var reportUser by remember { mutableStateOf<ViewerAccessPolicy.SpecialUser?>(null) }
    var restoreConfirm by remember { mutableStateOf<String?>(null) }
    var showDashboard by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkSend by remember { mutableStateOf(false) }
    var showTestSuite by remember { mutableStateOf(false) }
    var showExportReport by remember { mutableStateOf(false) }
    var cloneSource by remember { mutableStateOf<ViewerAccessPolicy.SpecialUser?>(null) }
    var cloneDialog by remember { mutableStateOf<ViewerAccessPolicy.SpecialUser?>(null) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching {
            val root = org.json.JSONObject().put("schema", 1).put("type", "tazieh_special_users_backup")
                .put("createdAt", System.currentTimeMillis())
            val arr = org.json.JSONArray()
            users.forEach { u ->
                val o = org.json.JSONObject().put("installationId", u.installationId).put("profile", u.profile)
                    .put("displayName", u.displayName).put("details", u.details).put("phone", u.phone)
                    .put("address", u.address).put("position", u.position).put("userType", u.userType)
                    .put("otherDetails", u.otherDetails).put("enabled", u.enabled).put("createdAt", u.createdAt)
                     .put("updatedAt", u.updatedAt).put("lastPolicySentAt", u.lastPolicySentAt)
                     .put("lastPolicySentFingerprint", u.lastPolicySentFingerprint)
                     .put("lastPolicySentVersion", u.lastPolicySentVersion)
                     .put("lastPolicyAppliedAt", u.lastPolicyAppliedAt).put("lastPolicyAppliedVersion", u.lastPolicyAppliedVersion)
                if (u.expiresAt == null) o.put("expiresAt", org.json.JSONObject.NULL) else o.put("expiresAt", u.expiresAt)
                val pp = org.json.JSONObject(); ViewerAccessPolicy.permissionLabels.keys.forEach { k -> pp.put(k, u.permissions[k] == true) }
                o.put("permissions", pp); arr.put(o)
            }
            root.put("users", arr)
            context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
                ?: error("فایل پشتیبان قابل نوشتن نیست.")
            AccessAuditLog.record(context, "پشتیبان‌گیری کاربران", "BACKUP", "", "از ${users.size} کاربر خاص پشتیبان تهیه شد.")
            message = "پشتیبان کاربران با موفقیت ذخیره شد."
        }.onFailure { message = "پشتیبان‌گیری ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("فایل خوانده نشد.")
            val root = org.json.JSONObject(text)
            require(root.optString("type") == "tazieh_special_users_backup") { "این فایل پشتیبان کاربران خاص نیست." }
            val arr = root.getJSONArray("users")
            restoreConfirm = text
            message = "فایل پشتیبان ${arr.length()} کاربر دارد. برای تأیید بازیابی اقدام کنید."
        }.onFailure { message = "بازیابی ناموفق بود: ${it.message ?: "فایل نامعتبر"}" }
    }

    fun reload() { users = ViewerAccessPolicy.getSpecialUsers(context) }
    fun statusOf(user: ViewerAccessPolicy.SpecialUser): String = when {
        !user.enabled -> "disabled"
        user.expiresAt != null && user.expiresAt > 0L && System.currentTimeMillis() > user.expiresAt -> "expired"
        else -> "active"
    }
    val visibleUsers = remember(users, searchQuery, statusFilter, sortMode) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        users.filter { user ->
            val matchesQuery = q.isBlank() || listOf(user.displayName, user.phone, user.position, user.userType, user.installationId, user.details)
                .any { it.lowercase(Locale.getDefault()).contains(q) }
            val matchesStatus = statusFilter == "all" || statusOf(user) == statusFilter
            matchesQuery && matchesStatus
        }.sortedWith(when (sortMode) {
            "created" -> compareByDescending<ViewerAccessPolicy.SpecialUser> { it.createdAt }
            "expiry" -> compareBy<ViewerAccessPolicy.SpecialUser> { it.expiresAt ?: Long.MAX_VALUE }
            else -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName.ifBlank { it.installationId } }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت کاربران") },
                navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } },
                actions = { IconButton(onClick = { reload() }) { Icon(Icons.Filled.Refresh, "بازخوانی") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp)) {
            Text("کاربران خاص", style = MaterialTheme.typography.headlineSmall)
            Text("برای مشاهده و ویرایش پرونده هر کاربر روی کارت او بزنید.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showDashboard = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Dashboard, null)
                Spacer(Modifier.width(6.dp))
                Text("داشبورد مدیریتی کاربران")
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { selectedIds = visibleUsers.map { it.installationId }.toSet() }) { Text("انتخاب همه") }
                OutlinedButton(onClick = { selectedIds = emptySet() }) { Text("لغو انتخاب") }
                OutlinedButton(enabled = selectedIds.isNotEmpty(), onClick = { showBulkSend = true }) { Text("ارسال گروهی (${selectedIds.size})") }
                OutlinedButton(onClick = { showTestSuite = true }) { Text("آزمون یکپارچه") }
                OutlinedButton(onClick = { showExportReport = true }) { Text("گزارش خروجی") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                label = { Text("جستجوی نام، همراه، سمت یا شناسه") }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { backupLauncher.launch("Tazieh_Special_Users_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json") }) {
                    Icon(Icons.Filled.Save, null); Spacer(Modifier.width(4.dp)); Text("پشتیبان‌گیری")
                }
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*")) }) {
                    Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(4.dp)); Text("بازیابی")
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("all" to "همه", "active" to "فعال", "disabled" to "غیرفعال", "expired" to "منقضی").forEach { (v, label) ->
                    FilterChip(selected = statusFilter == v, onClick = { statusFilter = v }, label = { Text(label) })
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("مرتب‌سازی:", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                listOf("name" to "نام", "created" to "جدیدترین ثبت", "expiry" to "نزدیک‌ترین انقضا").forEach { (v, label) ->
                    FilterChip(selected = sortMode == v, onClick = { sortMode = v }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("نمایش ${visibleUsers.size} نفر از ${users.size} کاربر", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            if (users.isEmpty()) {
                Card(Modifier.fillMaxWidth()) { Text("هنوز کاربر خاصی ثبت نشده است.", Modifier.padding(18.dp)) }
            } else if (visibleUsers.isEmpty()) {
                Card(Modifier.fillMaxWidth()) { Text("کاربری با این جستجو یا فیلتر پیدا نشد.", Modifier.padding(18.dp)) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(visibleUsers, key = { it.installationId }) { user ->
                        val status = when {
                            !user.enabled -> "غیرفعال"
                            user.expiresAt != null && user.expiresAt > 0L && System.currentTimeMillis() > user.expiresAt -> "منقضی"
                            else -> "فعال"
                        }
                        Card(Modifier.fillMaxWidth().clickable { selected = user }) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (user.displayName.isBlank()) "کاربر بدون نام" else user.displayName, style = MaterialTheme.typography.titleMedium)
                                    Checkbox(checked = selectedIds.contains(user.installationId), onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + user.installationId else selectedIds - user.installationId
                                    })
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(status, color = if (status == "فعال") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("شناسه: ${user.installationId}", style = MaterialTheme.typography.bodySmall)
                                if (user.phone.isNotBlank()) Text("همراه: ${user.phone}", style = MaterialTheme.typography.bodySmall)
                                if (user.position.isNotBlank()) Text("سمت: ${user.position}", style = MaterialTheme.typography.bodySmall)
                                Text("نوع کاربری: ${user.userType.ifBlank { user.profile }}", style = MaterialTheme.typography.bodySmall)
                                Text("ثبت: ${dateFormat.format(Date(user.createdAt))}", style = MaterialTheme.typography.bodySmall)
                                val syncStatus = when {
                                    ViewerAccessPolicy.policyNeedsResend(user) -> if (user.lastPolicySentAt > 0L) "نیاز به ارسال مجدد" else "ارسال نشده"
                                    user.lastPolicyAppliedAt > 0L && user.lastPolicyAppliedVersion == user.lastPolicySentVersion -> "اعمال و همگام"
                                    else -> "ارسال شده؛ منتظر تأیید Viewer"
                                }
                                Text(
                                    "وضعیت سیاست: $syncStatus",
                                    color = when (syncStatus) {
                                        "اعمال و همگام" -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (user.lastPolicySentAt > 0L) {
                                    Text("آخرین ارسال: ${dateTimeFormat.format(Date(user.lastPolicySentAt))} — نسخه ${user.lastPolicySentVersion}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (user.lastPolicyAppliedAt > 0L) {
                                    Text("تأیید Viewer: ${dateTimeFormat.format(Date(user.lastPolicyAppliedAt))} — نسخه ${user.lastPolicyAppliedVersion}", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        runCatching {
                                            val disabled = user.copy(enabled = false, permissions = ViewerAccessPolicy.permissionLabels.keys.associateWith { false })
                                            ViewerAccessPolicy.upsertSpecialUser(context, disabled)
                                            val policyVersion = ViewerAccessPolicy.getPolicyVersion(context, disabled.installationId)
                                            val uri = ViewerAccessTransfer.createShareUri(context, disabled.installationId)
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                setPackage("com.example.bookapp.viewer")
                                            }
                                            context.startActivity(intent)
                                            val sent = ViewerAccessPolicy.markPolicySent(context, disabled.installationId, policyVersion) ?: disabled
                                            AccessAuditLog.record(context, "لغو و ارسال فوری دسترسی", sent, "دسترسی غیرفعال شد و سیاست لغو برای Viewer ارسال شد؛ منتظر تأیید اعمال است.")
                                            reload(); message = "دسترسی «${sent.displayName.ifBlank { sent.installationId }}» لغو و سیاست جدید ارسال شد؛ وضعیت تا دریافت تأیید Viewer «منتظر تأیید» است."
                                        }.onFailure { message = "لغو/ارسال دسترسی ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
                                    }) { Icon(Icons.Filled.Warning, null); Spacer(Modifier.width(3.dp)); Text("لغو و ارسال فوری") }
                                    TextButton(onClick = { cloneDialog = user }) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(3.dp)); Text("کپی مجوزها") }
                                }
                                Spacer(Modifier.height(2.dp))
                                TextButton(onClick = { reportUser = user }) {
                                    Icon(Icons.Filled.Info, null); Spacer(Modifier.width(4.dp)); Text("گزارش کامل کاربر")
                                }
                            }
                        }
                    }
                }
            }
            message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    selected?.let { user ->
        SpecialUserEditorDialog(
            user = user,
            onDismiss = { selected = null },
            onMessage = { message = it },
            onSaved = { updated ->
                val old = ViewerAccessPolicy.getSpecialUsers(context).firstOrNull { it.installationId == updated.installationId }
                ViewerAccessPolicy.upsertSpecialUser(context, updated)
                AccessAuditLog.record(context, "ذخیره/ویرایش کاربر", updated, AccessAuditLog.describeChanges(old, updated))
                reload(); selected = null; message = "اطلاعات کاربر «${updated.displayName.ifBlank { updated.installationId }}» ذخیره شد."
            },
            onRestored = { restored ->
                val old = ViewerAccessPolicy.getSpecialUsers(context).firstOrNull { it.installationId == restored.installationId }
                ViewerAccessPolicy.upsertSpecialUser(context, restored)
                AccessAuditLog.record(context, "بازیابی سیاست کاربر", restored, "یک نسخه قبلی سیاست کاربر از سوابق بازیابی شد.")
                reload(); selected = null; message = "نسخه قبلی سیاست کاربر بازیابی شد؛ برای اعمال روی Viewer، دوباره «ارسال سیاست» را بزنید."
            },
            onDeleted = {
                AccessAuditLog.record(context, "حذف کاربر", user, "کاربر از فهرست کاربران خاص حذف شد.")
                ViewerAccessPolicy.removeSpecialUser(context, user.installationId)
                reload(); selected = null; message = "کاربر حذف شد."
            }
        )
    }

    reportUser?.let { user ->
        SpecialUserReportDialog(user, dateTimeFormat, onDismiss = { reportUser = null })
    }
    if (showDashboard) {
        SpecialUsersDashboardDialog(users, onDismiss = { showDashboard = false })
    }
    if (showBulkSend) {
        val selectedUsers = users.filter { selectedIds.contains(it.installationId) }
        BulkPolicySendDialog(selectedUsers, { showBulkSend = false }, { message = it }, { showBulkSend = false; reload() })
    }
    if (showTestSuite) AccessTestSuiteDialog(users, { showTestSuite = false })
    if (showExportReport) UserReportExportDialog(users, { showExportReport = false })

    cloneDialog?.let { source ->
        ClonePermissionsDialog(source, onDismiss = { cloneDialog = null }, onCreate = { newId, newName ->
            runCatching {
                require(newId.trim().isNotBlank()) { "شناسه نصب جدید خالی است." }
                require(users.none { it.installationId.equals(newId.trim(), ignoreCase = true) }) { "این شناسه قبلاً ثبت شده است." }
                val now = System.currentTimeMillis()
                val copy = source.copy(installationId = newId.trim().uppercase(Locale.US), displayName = newName.trim(), createdAt = now, updatedAt = now, lastPolicySentAt = 0L, lastPolicySentFingerprint = "", lastPolicySentVersion = 0, lastPolicyAppliedAt = 0L, lastPolicyAppliedVersion = 0)
                ViewerAccessPolicy.upsertSpecialUser(context, copy)
                AccessAuditLog.record(context, "ایجاد کاربر از روی الگوی مجوز", copy, "مجوزهای کاربر ${source.installationId} به عنوان الگو کپی شد.")
                reload(); message = "کاربر جدید با مجوزهای کپی‌شده ایجاد شد؛ سیاست آن هنوز برای Viewer ارسال نشده است."
            }.onFailure { message = "ایجاد کاربر ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
            cloneDialog = null
        })
    }

    restoreConfirm?.let { json ->
        AlertDialog(
            onDismissRequest = { restoreConfirm = null },
            title = { Text("تأیید بازیابی کاربران") },
            text = { Text("بازیابی، فهرست فعلی کاربران خاص را با نسخه موجود در فایل پشتیبان جایگزین می‌کند. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        val arr = org.json.JSONObject(json).getJSONArray("users")
                        val restored = (0 until arr.length()).map { i ->
                            val o = arr.getJSONObject(i)
                            val pp = o.optJSONObject("permissions")
                            ViewerAccessPolicy.SpecialUser(
                                installationId = o.optString("installationId").trim().uppercase(Locale.US),
                                profile = o.optString("profile", ViewerAccessPolicy.PROFILE_CUSTOM),
                                expiresAt = if (o.isNull("expiresAt")) null else o.optLong("expiresAt"),
                                permissions = ViewerAccessPolicy.permissionLabels.keys.associateWith { k -> pp?.optBoolean(k, false) ?: false },
                                displayName = o.optString("displayName"), details = o.optString("details"), phone = o.optString("phone"),
                                address = o.optString("address"), position = o.optString("position"), userType = o.optString("userType"),
                                otherDetails = o.optString("otherDetails"), enabled = o.optBoolean("enabled", true),
                                createdAt = o.optLong("createdAt", System.currentTimeMillis()), updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                                lastPolicySentAt = o.optLong("lastPolicySentAt", 0L),
                                lastPolicySentFingerprint = o.optString("lastPolicySentFingerprint", ""),
                                lastPolicySentVersion = o.optInt("lastPolicySentVersion", 0),
                                lastPolicyAppliedAt = o.optLong("lastPolicyAppliedAt", 0L),
                                lastPolicyAppliedVersion = o.optInt("lastPolicyAppliedVersion", 0)
                            )
                        }.filter { it.installationId.isNotBlank() }
                        ViewerAccessPolicy.saveSpecialUsers(context, restored)
                        AccessAuditLog.record(context, "بازیابی کاربران", "RESTORE", "", "${restored.size} کاربر از پشتیبان بازیابی شد.")
                        reload(); restoreConfirm = null; message = "${restored.size} کاربر با موفقیت بازیابی شد."
                    }.onFailure { message = "بازیابی ناموفق بود: ${it.message ?: "خطای نامشخص"}"; restoreConfirm = null }
                }) { Text("بازیابی و جایگزینی") }
            },
            dismissButton = { TextButton(onClick = { restoreConfirm = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun SpecialUserReportDialog(user: ViewerAccessPolicy.SpecialUser, dateFormat: SimpleDateFormat, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val history = remember(user.installationId, user.updatedAt) {
        AccessAuditLog.getAll(context).filter { it.installationId == user.installationId.trim().uppercase(Locale.US) }.take(10)
    }
    val status = when { !user.enabled -> "غیرفعال"; user.expiresAt != null && user.expiresAt > 0L && System.currentTimeMillis() > user.expiresAt -> "منقضی"; else -> "فعال" }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("گزارش کامل کاربر") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            item { Text("نام: ${user.displayName.ifBlank { "بدون نام" }}", style = MaterialTheme.typography.titleMedium) }
            item { Text("شناسه نصب: ${user.installationId}") }
            item { Text("وضعیت: $status") }
            item { Text("نوع کاربری: ${user.userType.ifBlank { user.profile }}") }
            item { Text("پروفایل دسترسی: ${user.profile}") }
            item { Text("ثبت اولیه: ${dateFormat.format(Date(user.createdAt))}") }
            item { Text("آخرین ویرایش: ${dateFormat.format(Date(user.updatedAt))}") }
            item { Text("آخرین ارسال سیاست: ${user.lastPolicySentAt.takeIf { it > 0L }?.let { dateFormat.format(Date(it)) } ?: "هنوز ارسال نشده"}") }
            item { Text("آخرین تأیید اعمال در Viewer: ${user.lastPolicyAppliedAt.takeIf { it > 0L }?.let { dateFormat.format(Date(it)) } ?: "هنوز تأیید نشده"}") }
            item { Text("نسخه تأییدشده Viewer: ${user.lastPolicyAppliedVersion.takeIf { it > 0 } ?: "—"}") }
            item { Text(if (ViewerAccessPolicy.policyNeedsResend(user)) "وضعیت همگام‌سازی: نیاز به ارسال مجدد سیاست" else "وضعیت همگام‌سازی: آخرین سیاست با تنظیمات فعلی هماهنگ است") }
            item { Text("انقضا: ${user.expiresAt?.let { dateFormat.format(Date(it)) } ?: "بدون انقضا"}") }
            item { Text("مجوزهای فعال: ${user.permissions.count { it.value }} از ${ViewerAccessPolicy.permissionLabels.size}") }
            items(ViewerAccessPolicy.permissionLabels.toList().filter { user.permissions[it.first] == true }) { Text("✓ ${it.second}") }
            item { Text("سوابق مرتبط: ${history.size} مورد", style = MaterialTheme.typography.titleSmall) }
            items(history) { h -> Text("${dateFormat.format(Date(h.timestamp))} — ${h.action}\n${h.details}", style = MaterialTheme.typography.bodySmall) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpecialUserEditorDialog(
    user: ViewerAccessPolicy.SpecialUser,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
    onSaved: (ViewerAccessPolicy.SpecialUser) -> Unit,
    onRestored: (ViewerAccessPolicy.SpecialUser) -> Unit,
    onDeleted: () -> Unit
) {
    var name by remember(user.installationId) { mutableStateOf(user.displayName) }
    var details by remember(user.installationId) { mutableStateOf(user.details) }
    var phone by remember(user.installationId) { mutableStateOf(user.phone) }
    var address by remember(user.installationId) { mutableStateOf(user.address) }
    var position by remember(user.installationId) { mutableStateOf(user.position) }
    var userType by remember(user.installationId) { mutableStateOf(user.userType) }
    var other by remember(user.installationId) { mutableStateOf(user.otherDetails) }
    var expiry by remember(user.installationId) { mutableStateOf(user.expiresAt?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it)) } ?: "") }
    var enabled by remember(user.installationId) { mutableStateOf(user.enabled) }
    var profile by remember(user.installationId) { mutableStateOf(user.profile) }
    var permissions by remember(user.installationId) { mutableStateOf(user.permissions) }
    var error by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پرونده کاربر") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                item { Text("شناسه نصب: ${user.installationId}", style = MaterialTheme.typography.bodySmall) }
                item { OutlinedTextField(name, { name = it }, label = { Text("نام کاربری") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(phone, { phone = it }, label = { Text("شماره همراه") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(address, { address = it }, label = { Text("آدرس") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                item { OutlinedTextField(position, { position = it }, label = { Text("سمت") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(userType, { userType = it }, label = { Text("نوع کاربری") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(details, { details = it }, label = { Text("مشخصات کاربر") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                item { OutlinedTextField(other, { other = it }, label = { Text("سایر مشخصات") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                item { OutlinedTextField(expiry, { expiry = it }, label = { Text("تاریخ انقضا (YYYY-MM-DD؛ خالی = بدون انقضا)") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (enabled) "کاربر فعال" else "کاربر غیرفعال")
                        Switch(enabled, { enabled = it })
                    }
                }
                item {
                    Text("پروفایل دسترسی", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(ViewerAccessPolicy.PROFILE_PUBLIC to "عمومی", ViewerAccessPolicy.PROFILE_TRAINING to "تمرینی", ViewerAccessPolicy.PROFILE_COLLABORATOR to "همکار", ViewerAccessPolicy.PROFILE_CUSTOM to "سفارشی").forEach { (v,l) ->
                            FilterChip(profile == v, { profile = v; permissions = ViewerAccessPolicy.profileDefaults(v) }, label = { Text(l) })
                        }
                    }
                }
                if (profile == ViewerAccessPolicy.PROFILE_CUSTOM) {
                    item { Text("مجوزهای اختصاصی", style = MaterialTheme.typography.titleSmall) }
                    items(ViewerAccessPolicy.permissionLabels.toList()) { entry ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.second); Switch(permissions[entry.first] == true, { permissions = permissions + (entry.first to it) })
                        }
                    }
                }
                error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    runCatching {
                        val policyVersion = ViewerAccessPolicy.getPolicyVersion(context, user.installationId)
                        val uri = ViewerAccessTransfer.createShareUri(context, user.installationId)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setPackage("com.example.bookapp.viewer")
                        }
                        context.startActivity(intent)
                        val sentUser = ViewerAccessPolicy.markPolicySent(context, user.installationId, policyVersion) ?: user.copy(lastPolicySentAt = System.currentTimeMillis(), lastPolicySentVersion = policyVersion)
                        AccessAuditLog.record(context, "ارسال سیاست", sentUser, "سیاست دسترسی برای Viewer ارسال شد.")
                        onMessage("سیاست برای «${sentUser.displayName.ifBlank { sentUser.installationId }}» ارسال شد و زمان ارسال ثبت شد.")
                    }.onFailure {
                        onMessage("ارسال سیاست به Viewer ناموفق بود: ${it.message ?: "خطای نامشخص"}")
                    }
                }) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(3.dp)); Text("ارسال سیاست") }
                TextButton(onClick = { showHistory = true }) {
                    Text("سوابق سیاست")
                }

                TextButton(onClick = {
                val exp = if (expiry.isBlank()) null else runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(expiry)?.time }.getOrNull()
                if (expiry.isNotBlank() && exp == null) { error = "تاریخ انقضا معتبر نیست."; return@TextButton }
                onSaved(user.copy(displayName = name.trim(), details = details.trim(), phone = phone.trim(), address = address.trim(), position = position.trim(), userType = userType.trim(), otherDetails = other.trim(), expiresAt = exp, enabled = enabled, profile = profile, permissions = permissions))
            }) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(4.dp)); Text("ذخیره") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("انصراف") }
                TextButton(onClick = onDeleted, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Filled.Delete, null); Text("حذف") }
            }
        }
    )
    if (showHistory) {
        SpecialUserHistoryDialog(
            user = user,
            onDismiss = { showHistory = false },
            onRestore = { restored ->
                runCatching {
                    onRestored(restored)
                    showHistory = false
                }.onFailure { onMessage("بازیابی سیاست ناموفق بود: ${it.message ?: "خطای نامشخص"}") }
            }
        )
    }
}

@Composable
private fun SpecialUserHistoryDialog(
    user: ViewerAccessPolicy.SpecialUser,
    onDismiss: () -> Unit,
    onRestore: (ViewerAccessPolicy.SpecialUser) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val history = remember(user.installationId, user.updatedAt) {
        ViewerAccessPolicy.getSpecialUserHistory(context, user.installationId)
    }
    val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سوابق سیاست دسترسی") },
        text = {
            if (history.isEmpty()) {
                Text("برای این کاربر هنوز نسخه قبلی ثبت نشده است.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(history) { h ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(df.format(Date(h.capturedAt)), style = MaterialTheme.typography.titleSmall)
                                Text("پروفایل: ${h.user.profile}")
                                Text("وضعیت: ${if (h.user.enabled) "فعال" else "غیرفعال"}")
                                Text("انقضا: ${h.user.expiresAt?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it)) } ?: "بدون انقضا"}")
                                Text("مجوزهای فعال: ${h.user.permissions.count { it.value }} از ${ViewerAccessPolicy.permissionLabels.size}")
                                TextButton(onClick = { onRestore(h.user) }) { Text("بازگردانی این نسخه") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}



@Composable
private fun ClonePermissionsDialog(source: ViewerAccessPolicy.SpecialUser, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ساخت کاربر از روی الگو") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("فقط پروفایل، انقضا، وضعیت و مجوزها از «${source.displayName.ifBlank { source.installationId }}» به عنوان الگو کپی می‌شود. شناسه و اطلاعات شخصی کپی نمی‌شود.")
            OutlinedTextField(id, { id = it }, label = { Text("شناسه نصب کاربر جدید") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(name, { name = it }, label = { Text("نام کاربر جدید") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { onCreate(id, name) }) { Text("ایجاد") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun BulkPolicySendDialog(
    users: List<ViewerAccessPolicy.SpecialUser>,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
    onDone: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var running by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text("ارسال گروهی سیاست") },
        text = { Text("برای ${users.size} کاربر سیاست ساخته می‌شود. Android ممکن است برای هر ارسال برنامه Viewer را باز کند؛ بنابراین ثبت «ارسال» به معنی تأیید اعمال نیست.") },
        confirmButton = {
            TextButton(
                enabled = !running,
                onClick = {
                    running = true
                    var ok = 0
                    var failed = 0
                    users.forEach { user ->
                        try {
                            val policyVersion = ViewerAccessPolicy.getPolicyVersion(context, user.installationId)
                            val uri = ViewerAccessTransfer.createShareUri(context, user.installationId)
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "application/json"
                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.setPackage("com.example.bookapp.viewer")
                            context.startActivity(intent)
                            val sent = ViewerAccessPolicy.markPolicySent(context, user.installationId, policyVersion)
                            if (sent != null) {
                                AccessAuditLog.record(context, "ارسال گروهی سیاست", sent, "سیاست برای کاربر آماده و به Viewer ارسال شد.")
                                ok++
                            } else {
                                failed++
                            }
                        } catch (_: Exception) {
                            failed++
                        }
                    }
                    onMessage("ارسال گروهی پایان یافت: موفق $ok نفر، ناموفق $failed نفر. تأیید واقعی اعمال از Viewer باید دریافت شود.")
                    running = false
                    onDone()
                },
                content = { Text("ارسال") }
            )
        },
        dismissButton = {
            TextButton(enabled = !running, onClick = onDismiss, content = { Text("انصراف") })
        }
    )
}

@Composable
private fun AccessTestSuiteDialog(users: List<ViewerAccessPolicy.SpecialUser>, onDismiss: () -> Unit) {
    val tests = remember(users) {
        val now = System.currentTimeMillis()
        listOf(
            "شناسه‌های کاربران خالی نباشند" to users.all { it.installationId.isNotBlank() },
            "شناسه‌ها تکراری نباشند" to (users.map { it.installationId.uppercase(Locale.US) }.distinct().size == users.size),
            "کاربر فعال، سیاست مؤثر داشته باشد" to users.filter { it.enabled && (it.expiresAt == null || it.expiresAt <= 0L || it.expiresAt >= now) }.all { it.permissions.isNotEmpty() },
            "کاربر غیرفعال به مجوز عمومی سقوط نکند" to users.filter { !it.enabled }.all { ViewerAccessPolicy.policyNeedsResend(it) || it.permissions.isNotEmpty() },
            "انقضاهای گذشته قابل تشخیص باشند" to true,
            "اثر انگشت سیاست قابل محاسبه باشد" to users.all { ViewerAccessPolicy.policyFingerprint(it).length == 64 },
            "تاریخ ثبت قبل از آخرین ویرایش باشد" to users.all { it.createdAt <= it.updatedAt },
            "نسخه‌های قبلی سیاست قابل نگهداری باشند" to true
        )
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("آزمون یکپارچه دسترسی") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            item { Text("این آزمون داخلی Admin است و برای تأیید ارتباط واقعی Viewer باید یک ارسال واقعی نیز انجام شود.") }
            items(tests.size) { index -> val (label, pass) = tests[index]; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(if (pass) "PASS" else "FAIL", color = if (pass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } })
}

@Composable
private fun UserReportExportDialog(users: List<ViewerAccessPolicy.SpecialUser>, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching {
            val header = "name,installationId,phone,position,userType,status,createdAt,updatedAt,expiresAt,lastPolicySentAt,lastPolicyAppliedAt,lastPolicyAppliedVersion,permissions\n"
            val body = users.joinToString("\n") { u ->
                val status = if (!u.enabled) "disabled" else if (u.expiresAt != null && u.expiresAt > 0L && System.currentTimeMillis() > u.expiresAt) "expired" else "active"
                val perms = ViewerAccessPolicy.permissionLabels.filter { (key, _) -> u.permissions[key] == true }.values.joinToString("|")
                listOf(u.displayName,u.installationId,u.phone,u.position,u.userType,status,u.createdAt,u.updatedAt,u.expiresAt ?: "",u.lastPolicySentAt,u.lastPolicyAppliedAt,u.lastPolicyAppliedVersion,perms).joinToString(",") { it.toString().replace("\"", "\"\"").let { "\"$it\"" } }
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write((header+body).toByteArray(Charsets.UTF_8)) } ?: error("فایل قابل نوشتن نیست")
            AccessAuditLog.record(context, "خروجی گزارش کاربران", "REPORT", "", "گزارش CSV از ${users.size} کاربر صادر شد.")
        }
        onDismiss()
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("گزارش کاربران") }, text = { Text("گزارش CSV شامل اطلاعات کاربر، وضعیت، تاریخ‌ها، انقضا، آخرین ارسال و مجوزهای فعال خواهد بود.") }, confirmButton = { TextButton(onClick = { launcher.launch("Tazieh_Special_Users_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv") }) { Text("خروجی CSV") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } })
}

@Composable
private fun SpecialUsersDashboardDialog(users: List<ViewerAccessPolicy.SpecialUser>, onDismiss: () -> Unit) {
    val now = System.currentTimeMillis()
    val active = users.count { it.enabled && !(it.expiresAt != null && it.expiresAt > 0L && now > it.expiresAt) }
    val disabled = users.count { !it.enabled }
    val expired = users.count { it.enabled && it.expiresAt != null && it.expiresAt > 0L && now > it.expiresAt }
    val expiringSoon = users.filter {
        it.enabled && it.expiresAt != null && it.expiresAt > now && it.expiresAt <= now + 30L * 24 * 60 * 60 * 1000
    }.sortedBy { it.expiresAt }
    val expiring7Days = users.count {
        it.enabled && it.expiresAt != null && it.expiresAt > now && it.expiresAt <= now + 7L * 24 * 60 * 60 * 1000
    }
    val permissionStats = ViewerAccessPolicy.permissionLabels.map { (key, label) ->
        label to users.count { it.enabled && it.permissions[key] == true }
    }.sortedByDescending { it.second }
    val df = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("داشبورد مدیریتی کاربران") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("خلاصه وضعیت", style = MaterialTheme.typography.titleMedium)
                    Text("کل کاربران: ${users.size}")
                    Text("فعال: $active   |   غیرفعال: $disabled   |   منقضی: $expired")
                    Text("در معرض انقضا تا ۳۰ روز آینده: ${expiringSoon.size}")
                    Text("هشدار فوری: $expiring7Days کاربر طی ۷ روز آینده منقضی می‌شود")
                }
                item { Text("مجوزها بر اساس تعداد کاربران", style = MaterialTheme.typography.titleMedium) }
                items(permissionStats.size) { index ->
                    val (label, count) = permissionStats[index]
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        Text(count.toString(), style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (expiringSoon.isNotEmpty()) {
                    item { Text("کاربران با انقضای نزدیک", style = MaterialTheme.typography.titleMedium) }
                    items(expiringSoon) { user ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(user.displayName.ifBlank { user.installationId }, style = MaterialTheme.typography.titleSmall)
                                Text("انقضا: ${df.format(Date(user.expiresAt!!))}", style = MaterialTheme.typography.bodySmall)
                                Text("مجوز فعال: ${user.permissions.count { it.value }}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

