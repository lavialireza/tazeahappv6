package com.example.bookapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AccessAuditLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessAuditLogScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entries by remember { mutableStateOf(AccessAuditLog.getAll(context)) }
    var confirmClear by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<AccessAuditLog.Entry?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var actionFilter by remember { mutableStateOf("همه") }
    var message by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    val actions = remember(entries) { listOf("همه") + entries.map { it.action }.distinct() }
    val visibleEntries = remember(entries, searchQuery, actionFilter) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        entries.filter { e ->
            val matchesQuery = q.isBlank() || listOf(e.action, e.installationId, e.displayName, e.details)
                .any { it.lowercase(Locale.getDefault()).contains(q) }
            val matchesAction = actionFilter == "همه" || e.action == actionFilter
            matchesQuery && matchesAction
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching {
            val arr = org.json.JSONArray()
            entries.forEach { e ->
                arr.put(org.json.JSONObject()
                    .put("timestamp", e.timestamp)
                    .put("action", e.action)
                    .put("installationId", e.installationId)
                    .put("displayName", e.displayName)
                    .put("details", e.details))
            }
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(arr.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("فایل قابل نوشتن نیست.")
            message = "گزارش JSON سوابق با موفقیت ذخیره شد."
        }.onFailure { message = "ذخیره گزارش ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) runCatching {
            fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
            val header = "timestamp,datetime,action,installationId,displayName,details\n"
            val body = entries.joinToString("\n") { e ->
                listOf(e.timestamp.toString(), dateFormat.format(Date(e.timestamp)), e.action, e.installationId, e.displayName, e.details)
                    .joinToString(",", transform = ::csv)
            }
            context.contentResolver.openOutputStream(uri)?.use {
                it.write((header + body).toByteArray(Charsets.UTF_8))
            } ?: error("فایل قابل نوشتن نیست.")
            message = "گزارش CSV سوابق با موفقیت ذخیره شد."
        }.onFailure { message = "ذخیره گزارش ناموفق بود: ${it.message ?: "خطای نامشخص"}" }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("سوابق تغییرات کاربران") },
            navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } },
            actions = {
                IconButton(onClick = { entries = AccessAuditLog.getAll(context) }) { Icon(Icons.Filled.Refresh, "بازخوانی") }
                IconButton(enabled = entries.isNotEmpty(), onClick = { jsonLauncher.launch("Tazieh_Access_Audit_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json") }) {
                    Icon(Icons.Filled.Share, "خروجی JSON")
                }
                IconButton(enabled = entries.isNotEmpty(), onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, "پاک کردن سوابق") }
            }
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                label = { Text("جستجو در کاربر، شناسه، عملیات یا جزئیات") }
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                actions.forEach { action ->
                    FilterChip(selected = actionFilter == action, onClick = { actionFilter = action }, label = { Text(action) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("نمایش ${visibleEntries.size} مورد از ${entries.size}", style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = entries.isNotEmpty(), onClick = { csvLauncher.launch("Tazieh_Access_Audit_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv") }) {
                    Text("خروجی CSV")
                }
            }
            Spacer(Modifier.height(4.dp))
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(20.dp)) { Text("هنوز سابقه‌ای ثبت نشده است.") }
            } else if (visibleEntries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(20.dp)) { Text("موردی با این جستجو یا فیلتر پیدا نشد.") }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleEntries) { e ->
                        Card(Modifier.fillMaxWidth().clickable { selectedEntry = e }) {
                            Column(Modifier.padding(12.dp)) {
                                Text(e.action, style = MaterialTheme.typography.titleMedium)
                                Text("کاربر: ${e.displayName.ifBlank { "بدون نام" }}", style = MaterialTheme.typography.bodyMedium)
                                Text("شناسه: ${e.installationId}", style = MaterialTheme.typography.bodySmall)
                                Text(dateFormat.format(Date(e.timestamp)), style = MaterialTheme.typography.bodySmall)
                                if (e.details.isNotBlank()) Text(e.details, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                Text("برای جزئیات کامل کلیک کنید", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("گزارش") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("باشه") } }
        )
    }

    selectedEntry?.let { e ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text("جزئیات سابقه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("عملیات: ${e.action}")
                    Text("کاربر: ${e.displayName.ifBlank { "بدون نام" }}")
                    Text("شناسه: ${e.installationId}")
                    Text("زمان: ${dateFormat.format(Date(e.timestamp))}")
                    if (e.details.isNotBlank()) Text("جزئیات: ${e.details}")
                }
            },
            confirmButton = { TextButton(onClick = { selectedEntry = null }) { Text("بستن") } }
        )
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("پاک کردن سوابق") },
        text = { Text("همه سوابق تغییرات کاربران خاص از این دستگاه حذف شود؟") },
        confirmButton = { TextButton(onClick = { AccessAuditLog.clear(context); entries = emptyList(); confirmClear = false }) { Text("پاک کردن") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("انصراف") } }
    )
}
