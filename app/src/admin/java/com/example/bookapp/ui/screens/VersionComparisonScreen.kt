package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.VersionComparisonReport
import com.example.bookapp.data.VersionComparisonService
import com.example.bookapp.data.MultiVersionComparisonPlan
import com.example.bookapp.data.archive.TaziehVersionEntity
import com.example.bookapp.data.TaziehEntity
import kotlinx.coroutines.launch

/**
 * مقایسه ۲ تا ۴ نسخه از یک تعزیه.
 * نسخه اول، مبنای مقایسه است و هر نسخه بعدی جداگانه با نسخه مبنا مقایسه می‌شود.
 * متن هیچ‌یک از نسخه‌ها در این صفحه تغییر نمی‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionComparisonScreen(db: AppDatabase, onBack: () -> Unit) {
    var taziehs by remember { mutableStateOf(emptyList<TaziehEntity>()) }
    var selectedTazieh by remember { mutableStateOf<TaziehEntity?>(null) }
    var versions by remember { mutableStateOf(emptyList<TaziehVersionEntity>()) }
    val selectedVersions = remember { mutableStateListOf<TaziehVersionEntity?> (null, null) }
    var reports by remember { mutableStateOf(emptyList<VersionComparisonReport>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { taziehs = db.taziehDao().getAll() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("مقایسه ۲ تا ۴ نسخه تعزیه") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            var taziehExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = taziehExpanded, onExpandedChange = { taziehExpanded = it }) {
                OutlinedTextField(
                    value = selectedTazieh?.title.orEmpty(), onValueChange = {}, readOnly = true,
                    label = { Text("انتخاب تعزیه") }, modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = taziehExpanded, onDismissRequest = { taziehExpanded = false }) {
                    taziehs.forEach { item ->
                        DropdownMenuItem(text = { Text(item.title) }, onClick = {
                            selectedTazieh = item
                            selectedVersions.clear(); selectedVersions.addAll(listOf(null, null))
                            reports = emptyList(); message = null; taziehExpanded = false
                            scope.launch { versions = db.archiveDao().versionsForTazieh(item.uid) }
                        })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("حداقل ۲ و حداکثر ۴ نسخه را انتخاب کنید.", style = MaterialTheme.typography.bodySmall)
            Text("نسخه اول مبناست؛ اختلاف هر نسخه دیگر نسبت به نسخه اول نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))

            selectedVersions.forEachIndexed { index, selected ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VersionDropdown(
                        label = if (index == 0) "نسخه مبنا" else "نسخه ${index + 1}",
                        versions = versions,
                        selected = selected,
                        excludedUids = selectedVersions.mapIndexedNotNull { otherIndex, item -> if (otherIndex != index) item?.uid else null }.toSet(),
                        modifier = Modifier.weight(1f)
                    ) { choice ->
                        selectedVersions[index] = choice
                        reports = emptyList(); message = null
                    }
                    if (selectedVersions.size > 2) {
                        TextButton(onClick = {
                            selectedVersions.removeAt(index)
                            reports = emptyList(); message = null
                        }) { Text("حذف") }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (selectedVersions.size < 4 && versions.size >= selectedVersions.size + 1) {
                OutlinedButton(
                    onClick = { selectedVersions.add(null); reports = emptyList(); message = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("افزودن نسخه دیگر (${selectedVersions.size + 1} از ۴)") }
            }

            val chosen = selectedVersions.filterNotNull()
            val allSelected = chosen.size == selectedVersions.size && chosen.size in 2..4 && chosen.map { it.uid }.distinct().size == chosen.size
            Spacer(Modifier.height(8.dp))
            Button(enabled = !busy && allSelected, onClick = {
                scope.launch {
                    busy = true; message = null; reports = emptyList()
                    try {
                        val plan = MultiVersionComparisonPlan.pairs(chosen.map { it.uid })
                        val comparisonService = VersionComparisonService(db)
                        reports = plan.map { (baseUid, comparisonUid) ->
                            comparisonService.compare(baseUid, comparisonUid)
                        }
                    } catch (e: Exception) {
                        message = e.message ?: "مقایسه نسخه‌ها ناموفق بود."
                    } finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "در حال مقایسه نسخه‌ها…" else "مقایسه ${chosen.size} نسخه")
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (reports.isNotEmpty()) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(reports) { reportIndex, result ->
                        val comparisonVersion = chosen.getOrNull(reportIndex + 1)
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    "مقایسه مبنا «${chosen.first().title}» با «${comparisonVersion?.title.orEmpty()}»",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "بخش‌های مشترک: ${result.matchedSections} | افزوده: ${result.addedSections} | حذف‌شده: ${result.removedSections}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                result.sectionDiffs.forEach { item ->
                                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                                    Text("${item.roleTitle} ← ${item.sectionTitle}", style = MaterialTheme.typography.titleSmall)
                                    Text("یکسان: ${item.diff.same} | افزوده: ${item.diff.added} | حذف: ${item.diff.removed}", style = MaterialTheme.typography.labelSmall)
                                    item.diff.lines.take(250).forEach { line ->
                                        val prefix = when (line.kind) {
                                            com.example.bookapp.data.DiffKind.SAME -> "  "
                                            com.example.bookapp.data.DiffKind.ADDED -> "+ "
                                            com.example.bookapp.data.DiffKind.REMOVED -> "− "
                                        }
                                        Text(prefix + line.text, style = MaterialTheme.typography.bodySmall,
                                            color = when (line.kind) {
                                                com.example.bookapp.data.DiffKind.ADDED -> MaterialTheme.colorScheme.primary
                                                com.example.bookapp.data.DiffKind.REMOVED -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurface
                                            })
                                    }
                                    if (item.diff.lines.size > 250) Text("نمایش به ۲۵۰ سطر اول این بخش محدود شده است.", style = MaterialTheme.typography.labelSmall)
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
private fun VersionDropdown(
    label: String,
    versions: List<TaziehVersionEntity>,
    selected: TaziehVersionEntity?,
    excludedUids: Set<String>,
    modifier: Modifier,
    onSelect: (TaziehVersionEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.let { "${it.versionNumber}. ${it.title}" } ?: label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            versions.filter { it.uid == selected?.uid || it.uid !in excludedUids }.forEach { item ->
                DropdownMenuItem(
                    text = { Text("نسخه ${item.versionNumber}: ${item.title}${if (item.isCurrent) " (جاری)" else ""}") },
                    onClick = { onSelect(item); expanded = false }
                )
            }
        }
    }
}
