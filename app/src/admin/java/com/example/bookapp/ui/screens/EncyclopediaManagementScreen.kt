package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.example.bookapp.data.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaManagementScreen(db: AppDatabase, onBack: () -> Unit, onOpenGraph: () -> Unit = {}) {
    val dao = db.encyclopediaDao()
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(EncyclopediaRecordEntity.PERSON) }
    var filterKind by remember { mutableStateOf("ALL") }
    var status by remember { mutableStateOf("ALL") }
    var query by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var aliases by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var citation by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var taziehUid by remember { mutableStateOf("") }
    var versionUid by remember { mutableStateOf("") }
    var roleUid by remember { mutableStateOf("") }
    var sectionUid by remember { mutableStateOf("") }
    var relationNote by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(emptyList<EncyclopediaRecordEntity>()) }
    var selected by remember { mutableStateOf<EncyclopediaRecordEntity?>(null) }
    var selectedLinks by remember { mutableStateOf(emptyList<EncyclopediaLinkEntity>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    suspend fun reload() { records = dao.search(filterKind, status, normalizeEncyclopediaText(query)) }
    LaunchedEffect(filterKind, status, query) { reload() }

    Scaffold(topBar = { TopAppBar(title = { Text("دانشنامه تخصصی تعزیه") }, navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { Text("پرونده‌های دانشنامه‌ای", style = MaterialTheme.typography.titleLarge) }
            item { OutlinedButton(onClick = onOpenGraph, modifier = Modifier.fillMaxWidth()) { Text("نمایش نقشه ارتباطات مدخل‌ها با آرشیو") } }
            item { Text("اشخاص و شخصیت‌ها، وقایع، مکان‌ها و منابع؛ هر مدخل تا زمان تأیید در وضعیت پیشنهادی باقی می‌ماند.", style = MaterialTheme.typography.bodySmall) }
            item { Text("نوع مدخل") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(EncyclopediaRecordEntity.PERSON to "شخص/شخصیت", EncyclopediaRecordEntity.EVENT to "واقعه", EncyclopediaRecordEntity.PLACE to "مکان", EncyclopediaRecordEntity.SOURCE to "منبع").forEach { (v, label) -> FilterChip(selected = kind == v, onClick = { kind = v }, label = { Text(label) }) }
            } }
            item { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("عنوان اصلی *") }, singleLine = true) }
            item { OutlinedTextField(aliases, { aliases = it }, Modifier.fillMaxWidth(), label = { Text("نام‌های دیگر / صورت‌های جایگزین") }) }
            item { OutlinedTextField(summary, { summary = it }, Modifier.fillMaxWidth(), label = { Text("خلاصه معرفی") }, minLines = 2) }
            item { OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text("شرح تفصیلی و یادداشت پژوهشی") }, minLines = 3) }
            item { OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("تاریخ / بازه زمانی (با ذکر تقویم یا تقریبی بودن)") }) }
            item { OutlinedTextField(place, { place = it }, Modifier.fillMaxWidth(), label = { Text("مکان مرتبط") }) }
            item { OutlinedTextField(citation, { citation = it }, Modifier.fillMaxWidth(), label = { Text("استناد کتابشناختی / آرشیوی") }, minLines = 2) }
            item { OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("پیوند منبع (اختیاری)") }) }
            item { Text("پیوند به آرشیو تعزیه (شناسه‌های پایدار؛ هر مورد اختیاری)", style = MaterialTheme.typography.titleSmall) }
            item { OutlinedTextField(taziehUid, { taziehUid = it }, Modifier.fillMaxWidth(), label = { Text("شناسه تعزیه UID") }, singleLine = true) }
            item { OutlinedTextField(versionUid, { versionUid = it }, Modifier.fillMaxWidth(), label = { Text("شناسه نسخه UID") }, singleLine = true) }
            item { OutlinedTextField(roleUid, { roleUid = it }, Modifier.fillMaxWidth(), label = { Text("شناسه نقش UID") }, singleLine = true) }
            item { OutlinedTextField(sectionUid, { sectionUid = it }, Modifier.fillMaxWidth(), label = { Text("شناسه بخش UID") }, singleLine = true) }
            item { OutlinedTextField(relationNote, { relationNote = it }, Modifier.fillMaxWidth(), label = { Text("توضیح نوع ارتباط / محل شاهد") }) }
            item { Button(enabled = !busy && title.isNotBlank(), onClick = {
                scope.launch {
                    busy = true; message = null
                    try {
                        val now = System.currentTimeMillis()
                        val normalized = normalizeEncyclopediaText(title)
                        require(normalized.isNotBlank()) { "عنوان معتبر نیست." }
                        require(url.isBlank() || url.startsWith("https://") || url.startsWith("http://")) { "پیوند منبع باید با http یا https آغاز شود." }
                        db.withTransaction {
                            require(listOf(taziehUid, versionUid, roleUid, sectionUid).none { it.isNotBlank() } || taziehUid.isNotBlank()) { "برای پیوند نسخه/نقش/بخش، شناسه تعزیه نیز لازم است." }
                            val existing = editingId?.let { dao.record(it) }
                            val entity = if (existing == null) EncyclopediaRecordEntity(uid = "ency-${UUID.randomUUID()}", kind = kind, title = title.trim(), normalizedTitle = normalized, aliases = aliases.trim(), summary = summary.trim(), details = details.trim(), dateLabel = date.trim(), placeLabel = place.trim(), sourceCitation = citation.trim(), externalUrl = url.trim(), createdAt = now, updatedAt = now) else existing.copy(kind = kind, title = title.trim(), normalizedTitle = normalized, aliases = aliases.trim(), summary = summary.trim(), details = details.trim(), dateLabel = date.trim(), placeLabel = place.trim(), sourceCitation = citation.trim(), externalUrl = url.trim(), updatedAt = now)
                            val id = if (existing == null) dao.insertRecord(entity) else { dao.updateRecord(entity); existing.id }
                            if (existing != null) dao.deleteLinksForRecord(id)
                            if (taziehUid.isNotBlank()) dao.insertLink(EncyclopediaLinkEntity(recordId = id, taziehUid = taziehUid.trim(), versionUid = versionUid.trim(), roleUid = roleUid.trim(), sectionUid = sectionUid.trim(), note = relationNote.trim()))
                            dao.insertHistory(EncyclopediaHistoryEntity(recordId = id, action = if (existing == null) "CREATE" else "UPDATE", beforeJson = existing?.let { JSONObject().put("title", it.title).put("summary", it.summary).toString() } ?: "{}", afterJson = JSONObject().put("kind", kind).put("title", title.trim()).put("citation", citation.trim()).toString(), changedAt = now))
                        }
                        editingId = null; title = ""; aliases = ""; summary = ""; details = ""; date = ""; place = ""; citation = ""; url = ""; taziehUid = ""; versionUid = ""; roleUid = ""; sectionUid = ""; relationNote = ""
                        reload(); message = "مدخل دانشنامه ثبت شد و تا زمان بررسی، پیشنهادی است."
                    } catch (e: Exception) { message = "ثبت انجام نشد: ${e.message ?: "خطای نامشخص"}" }
                    finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "در حال ذخیره…" else if (editingId != null) "ذخیره ویرایش مدخل" else "ثبت مدخل دانشنامه") } }
            item { message?.let { Text(it, color = MaterialTheme.colorScheme.primary) } }
            item { Divider(); Text("جستجو و بررسی مدخل‌ها", style = MaterialTheme.typography.titleMedium) }
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("جستجو در عنوان، نام‌های دیگر و شرح") }) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("ALL" to "همه انواع", "PERSON" to "اشخاص", "EVENT" to "وقایع", "PLACE" to "مکان‌ها", "SOURCE" to "منابع").forEach { (v, label) -> FilterChip(selected = filterKind == v, onClick = { filterKind = v }, label = { Text(label) }) } } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("ALL" to "همه", "PENDING" to "در انتظار", "VERIFIED" to "تأیید", "REJECTED" to "ردشده").forEach { (v, label) -> FilterChip(selected = status == v, onClick = { status = v }, label = { Text(label) }) } } }
            items(records, key = { it.id }) { record ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(record.title, style = MaterialTheme.typography.titleMedium)
                        Text("${kindLabel(record.kind)} • ${statusLabel(record.reviewStatus)}", style = MaterialTheme.typography.labelSmall)
                        if (record.aliases.isNotBlank()) Text("نام‌های دیگر: ${record.aliases}")
                        if (record.summary.isNotBlank()) Text(record.summary)
                        if (record.dateLabel.isNotBlank() || record.placeLabel.isNotBlank()) Text(listOf(record.dateLabel, record.placeLabel).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                        if (record.sourceCitation.isNotBlank()) Text("منبع: ${record.sourceCitation}", style = MaterialTheme.typography.bodySmall)
                        if (record.details.isNotBlank()) Text(record.details)
                        Row {
                            TextButton(onClick = {
                                editingId = record.id; kind = record.kind; title = record.title; aliases = record.aliases; summary = record.summary; details = record.details; date = record.dateLabel; place = record.placeLabel; citation = record.sourceCitation; url = record.externalUrl
                                scope.launch { val link = dao.links(record.id).firstOrNull(); taziehUid = link?.taziehUid.orEmpty(); versionUid = link?.versionUid.orEmpty(); roleUid = link?.roleUid.orEmpty(); sectionUid = link?.sectionUid.orEmpty(); relationNote = link?.note.orEmpty() }
                            }) { Text("ویرایش") }
                            TextButton(onClick = { scope.launch { selected = record; selectedLinks = dao.links(record.id) } }) { Text("پیوندها و تاریخچه") }
                            if (record.reviewStatus != EncyclopediaRecordEntity.VERIFIED) TextButton(onClick = { scope.launch { val before = record.reviewStatus; val now = System.currentTimeMillis(); dao.updateRecord(record.copy(reviewStatus = EncyclopediaRecordEntity.VERIFIED, updatedAt = now)); dao.insertHistory(EncyclopediaHistoryEntity(recordId = record.id, action = "VERIFY", beforeJson = "{\"status\":\"$before\"}", afterJson = "{\"status\":\"VERIFIED\"}", changedAt = now)); reload() } }) { Text("تأیید") }
                            if (record.reviewStatus != EncyclopediaRecordEntity.REJECTED) TextButton(onClick = { scope.launch { val before = record.reviewStatus; val now = System.currentTimeMillis(); dao.updateRecord(record.copy(reviewStatus = EncyclopediaRecordEntity.REJECTED, updatedAt = now)); dao.insertHistory(EncyclopediaHistoryEntity(recordId = record.id, action = "REJECT", beforeJson = "{\"status\":\"$before\"}", afterJson = "{\"status\":\"REJECTED\"}", changedAt = now)); reload() } }) { Text("رد") }
                        }
                        if (selected?.id == record.id) {
                            Divider(); Text("پیوندهای آرشیو", style = MaterialTheme.typography.titleSmall)
                            if (selectedLinks.isEmpty()) Text("پیوندی ثبت نشده است.")
                            selectedLinks.forEach { link -> Text("تعزیه: ${link.taziehUid} | نسخه: ${link.versionUid} | نقش: ${link.roleUid} | بخش: ${link.sectionUid}\n${link.note}", style = MaterialTheme.typography.bodySmall) }
                            val history = remember(record.id) { mutableStateOf(emptyList<EncyclopediaHistoryEntity>()) }
                            LaunchedEffect(record.id) { history.value = dao.history(record.id) }
                            Text("تاریخچه: ${history.value.size} رویداد", style = MaterialTheme.typography.labelSmall)
                            history.value.take(5).forEach { h -> Text("${h.action} • ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(h.changedAt))}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            if (records.isEmpty()) item { Text("مدخلی مطابق فیلترها پیدا نشد.") }
        }
    }
}

private fun kindLabel(kind: String) = when (kind) { "PERSON" -> "شخص/شخصیت"; "EVENT" -> "واقعه"; "PLACE" -> "مکان"; "SOURCE" -> "منبع"; else -> kind }
private fun statusLabel(status: String) = when (status) { "VERIFIED" -> "تأییدشده"; "REJECTED" -> "ردشده"; else -> "در انتظار بررسی" }
