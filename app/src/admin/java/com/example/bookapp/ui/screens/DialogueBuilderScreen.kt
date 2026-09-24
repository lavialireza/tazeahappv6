package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * صفحه‌ی ساخت گفتگو: از بین همه‌ی بخش‌های تعزیه (با نام نقششان)، کاربر به
 * ترتیبی که مکالمه پیش می‌رود لمس می‌کند (مثلاً اول بخش امام حسین، بعد
 * بخش علی‌اکبر، بعد دوباره امام حسین ...) و در پایان یک نام برای گفتگو
 * می‌گذارد و ذخیره می‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueBuilderScreen(
    allSections: List<SectionPickerItem>,
    onSave: (title: String, orderedSectionIds: List<Long>) -> Unit,
    onBack: () -> Unit
) {
    var selectedSequence by remember { mutableStateOf(listOf<SectionPickerItem>()) }
    var title by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ساخت گفتگوی جدید") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("نام گفتگو (مثلاً «گفتگوی شمر و عباس»)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Text(
                "ترتیب انتخاب‌شده (${selectedSequence.size} نوبت):",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (selectedSequence.isEmpty()) {
                Text(
                    "از فهرست پایین، بخش‌ها را به ترتیبی که در مکالمه پیش می‌روند لمس کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedSequence.withIndex().toList(), key = { it.index }) { (index, item) ->
                        AssistChip(
                            onClick = {
                                selectedSequence = selectedSequence.toMutableList().apply { removeAt(index) }
                            },
                            label = { Text("${index + 1}. ${item.roleTitle}") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "حذف از توالی") }
                        )
                    }
                }
            }

            HorizontalDivider()
            Text(
                "همه‌ی بخش‌های این تعزیه:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allSections, key = { it.sectionId }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { selectedSequence = selectedSequence + item }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.roleTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(item.sectionTitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Button(
                onClick = { onSave(title.trim(), selectedSequence.map { it.sectionId }) },
                enabled = title.isNotBlank() && selectedSequence.size >= 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("ذخیره گفتگو")
            }
            if (title.isBlank() || selectedSequence.size < 2) {
                Text(
                    when {
                        title.isBlank() && selectedSequence.size < 2 ->
                            "برای فعال‌شدن: یک نام برای گفتگو بنویسید و حداقل ۲ بخش از فهرست پایین انتخاب کنید."
                        title.isBlank() -> "برای فعال‌شدن: یک نام برای گفتگو بنویسید."
                        else -> "برای فعال‌شدن: حداقل ۲ بخش از فهرست پایین انتخاب کنید (الان ${selectedSequence.size} تا)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
