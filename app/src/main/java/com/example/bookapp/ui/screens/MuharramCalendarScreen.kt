package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.MuharramCountdown

data class MuharramTaziehSuggestion(
    val eventTitle: String,
    val taziehId: Long,
    val taziehTitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuharramCalendarScreen(
    countdowns: List<MuharramCountdown>?,
    suggestions: List<MuharramTaziehSuggestion>,
    onOpenTazieh: (Long) -> Unit,
    onBack: () -> Unit,
    showSuggestions: Boolean = true
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تقویم محرم") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (countdowns == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    "این قابلیت روی نسخه‌ی فعلی اندروید گوشی شما در دسترس نیست (نیاز به اندروید ۷ یا بالاتر).",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(countdowns) { c ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (c.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(14.dp).fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(c.event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (c.isToday) "امروز 🖤" else "${c.daysRemaining} روز مانده",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(c.event.description, style = MaterialTheme.typography.bodySmall)

                        val related = suggestions.filter { it.eventTitle == c.event.title }
                        if (related.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            related.forEach { s ->
                                OutlinedButton(onClick = { onOpenTazieh(s.taziehId) }) {
                                    Text("مشاهده‌ی «${s.taziehTitle}»")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
