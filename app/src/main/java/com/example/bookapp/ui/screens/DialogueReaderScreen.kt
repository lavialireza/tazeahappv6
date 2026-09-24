package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DialogueTurnDisplay(
    val turnId: Long,
    val sectionId: Long,
    val roleTitle: String,
    val sectionTitle: String,
    val content: String
)

/**
 * نمایش یک گفتگو به سبک نمایش‌نامه: هر نوبت با نام نقشش، پشت سر هم.
 * قابل ویرایش: جابه‌جایی ترتیب نوبت‌ها یا حذف یک نوبت از گفتگو
 * (بخش اصلی در دیتابیس حذف نمی‌شود، فقط از این گفتگو خارج می‌شود).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueReaderScreen(
    dialogueTitle: String,
    turns: List<DialogueTurnDisplay>,
    onMoveTurn: (index: Int, direction: Int) -> Unit,
    onDeleteTurn: (DialogueTurnDisplay) -> Unit,
    onAddTurn: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onBack: () -> Unit,
    readOnly: Boolean = false,
    showPdf: Boolean = true
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dialogueTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (!readOnly) {
                        IconButton(onClick = onAddTurn) {
                            Icon(Icons.Filled.Add, contentDescription = "افزودن نوبت")
                        }
                    }
                    if (showPdf) {
                        IconButton(onClick = onExportPdf) {
                            Icon(Icons.Filled.Share, contentDescription = "خروجی PDF گفتگو")
                        }
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
            // به هر نقش، به ترتیب ظاهر شدنش در گفتگو، یکی از دو رنگ پس‌زمینه ثابت
            // اختصاص داده می‌شود تا نقش‌های مختلف در گفتگو به‌وضوح از هم قابل تشخیص باشند
            val roleColorIndex = remember(turns) {
                val seen = linkedSetOf<String>()
                turns.forEach { seen.add(it.roleTitle) }
                seen.withIndex().associate { (i, roleTitle) -> roleTitle to (i % 2) }
            }

            turns.forEachIndexed { index, turn ->
                val isSecondColor = roleColorIndex[turn.roleTitle] == 1
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSecondColor) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                "${turn.roleTitle}:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!readOnly) {
                                Row {
                                    IconButton(onClick = { onMoveTurn(index, -1) }, enabled = index > 0) {
                                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "جابه‌جایی به بالا")
                                    }
                                    IconButton(onClick = { onMoveTurn(index, 1) }, enabled = index < turns.size - 1) {
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "جابه‌جایی به پایین")
                                    }
                                    IconButton(onClick = { onDeleteTurn(turn) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "حذف این نوبت از گفتگو")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(turn.content, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
