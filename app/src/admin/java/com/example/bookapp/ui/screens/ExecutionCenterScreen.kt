package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** ورودی مستقل ابزارهای اداره گروه و اجرای تعزیه؛ مختص Admin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionCenterScreen(
    onOpenGroupManagement: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مرکز اجرا") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                        Text("مدیریت چرخه اجرای تعزیه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "این محیط برای اداره گروه، شبیه‌خوان‌ها، تخصیص نقش، تمرین و ثبت سوابق اجرا طراحی شده است. هر پرونده اجرا باید به تعزیه و نسخه مشخصی از متن متصل باشد.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Groups, contentDescription = null)
                        Text("گروه‌ها و شبیه‌خوان‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("مدیریت گروه‌ها، اعضا، نقش‌ها و اطلاعات اجرایی.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onOpenGroupManagement, modifier = Modifier.fillMaxWidth()) {
                            Text("ورود به مدیریت گروه‌ها")
                        }
                    }
                }
            }
        }
    }
}
