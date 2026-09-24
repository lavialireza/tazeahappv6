package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChangelogEntry(val version: String, val changes: List<String>)

/**
 * فهرست تغییرات هر نسخه، برای اینکه کاربر ببیند بعد از هر بروزرسانی چه چیز
 * جدیدی اضافه شده. این لیست به‌صورت دستی در کد نگه‌داری می‌شود؛ با هر تغییر
 * قابل‌توجه، یک خط به بالای فهرست زیر (در AppNavigation.kt) اضافه کنید.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(entries: List<ChangelogEntry>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("چه چیزی جدید است؟") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            entries.forEach { entry ->
                Text("نسخه ${entry.version}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                entry.changes.forEach { change ->
                    Text("•  $change", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
