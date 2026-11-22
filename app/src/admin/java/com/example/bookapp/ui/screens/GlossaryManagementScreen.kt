package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.room.withTransaction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private fun normalizeGlossaryText(value: String): String = value.trim()
    .replace('ي', 'ی').replace('ك', 'ک').replace('ۀ', 'ه').replace('ة', 'ه')
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace(Regex("\\s+"), " ")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryManagementScreen(db: AppDatabase, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var headword by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf("") }
    var spelling by remember { mutableStateOf("") }
    var partOfSpeech by remember { mutableStateOf("") }
    var pronunciation by remember { mutableStateOf("") }
    var etymology by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var taziehUid by remember { mutableStateOf("") }
    var versionUid by remember { mutableStateOf("") }
    var roleUid by remember { mutableStateOf("") }
    var sectionUid by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(emptyList<GlossaryTermEntity>()) }
    var selected by remember { mutableStateOf<GlossaryTermEntity?>(null) }
    var selectedMeanings by remember { mutableStateOf(emptyList<GlossaryMeaningEntity>()) }
    var statusFilter by remember { mutableStateOf("ALL") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    suspend fun reload() {
        terms = if (statusFilter == "ALL") db.glossaryDao().allTerms() else db.glossaryDao().termsByStatus(statusFilter)
        selected?.let { selectedMeanings = db.glossaryDao().meanings(it.id) }
    }
    LaunchedEffect(statusFilter) { reload() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("واژه‌نامه تخصصی تعزیه") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("ثبت مدخل و معنی", style = MaterialTheme.typography.titleMedium)
                Text("هر مدخل می‌تواند چند معنی مستقل، صورت نوشتاری و شاهد مستند داشته باشد. اطلاعات تا زمان بررسی، پیشنهادی محسوب می‌شود.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(headword, { headword = it }, label = { Text("واژه / سرمدخل") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(spelling, { spelling = it }, label = { Text("صورت نوشتاری دیگر (اختیاری)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(partOfSpeech, { partOfSpeech = it }, label = { Text("نوع واژه (اسم، فعل، اصطلاح و…)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pronunciation, { pronunciation = it }, label = { Text("تلفظ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(etymology, { etymology = it }, label = { Text("ریشه‌شناسی") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(meaning, { meaning = it }, label = { Text("معنی") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(source, { source = it }, label = { Text("منبع معنی / عنوان کتاب") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(author, { author = it }, label = { Text("پدیدآورنده") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(page, { page = it }, label = { Text("صفحه / جلد") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sourceUrl, { sourceUrl = it }, label = { Text("پیوند منبع (اختیاری)") }, modifier = Modifier.fillMaxWidth())
                Text("نشانی شاهد در مجموعه تعزیه (در صورت وجود)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(taziehUid, { taziehUid = it }, label = { Text("شناسه تعزیه") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(versionUid, { versionUid = it }, label = { Text("شناسه نسخه") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(roleUid, { roleUid = it }, label = { Text("شناسه نقش") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sectionUid, { sectionUid = it }, label = { Text("شناسه بخش") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(quote, { quote = it }, label = { Text("شاهد متنی یا کاربرد در تعزیه") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Button(enabled = !busy && headword.isNotBlank() && meaning.isNotBlank(), onClick = {
                    scope.launch {
                        busy = true; message = null
                        try {
                            val normalized = normalizeGlossaryText(headword)
                            require(normalized.isNotBlank()) { "سرمدخل معتبر نیست." }
                            db.withTransaction {
                                val dao = db.glossaryDao()
                                val existing = dao.byNormalizedHeadword(normalized)
                                val term = existing ?: GlossaryTermEntity(
                                    uid = "term-${UUID.randomUUID()}", headword = headword.trim(), normalizedHeadword = normalized,
                                    partOfSpeech = partOfSpeech.trim(), pronunciation = pronunciation.trim(), etymology = etymology.trim(),
                                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                                ).let { draft -> draft.copy(id = dao.insertTerm(draft)) }
                                if (spelling.isNotBlank()) {
                                    val normalizedSpelling = normalizeGlossaryText(spelling)
                                    require(normalizedSpelling != normalized) { "صورت نوشتاری با خود سرمدخل یکسان است." }
                                    if (dao.spellings(term.id).none { it.normalizedSpelling == normalizedSpelling })
                                        dao.insertSpelling(GlossarySpellingEntity(termId = term.id, spelling = spelling.trim(), normalizedSpelling = normalizedSpelling))
                                }
                                require(dao.duplicateMeaning(term.id, meaning.trim(), source.trim(), author.trim(), page.trim()) == null) { "این معنی با همین مشخصات منبع قبلاً ثبت شده است." }
                                val now = System.currentTimeMillis()
                                val m = GlossaryMeaningEntity(uid = "meaning-${UUID.randomUUID()}", termId = term.id, meaning = meaning.trim(), sourceTitle = source.trim(), sourceAuthor = author.trim(), sourcePage = page.trim(), sourceUrl = sourceUrl.trim(),
                                    usageTaziehUid = taziehUid.trim().ifBlank { null }, usageVersionUid = versionUid.trim().ifBlank { null },
                                    usageRoleUid = roleUid.trim().ifBlank { null }, usageSectionUid = sectionUid.trim().ifBlank { null }, usageQuote = quote.trim(), createdAt = now, updatedAt = now)
                                dao.insertMeaning(m)
                                dao.insertHistory(GlossaryHistoryEntity(termId = term.id, action = if (existing == null) "CREATE_TERM_AND_MEANING" else "ADD_MEANING", beforeJson = "{}", afterJson = JSONObject().put("headword", term.headword).put("meaning", m.meaning).put("source", m.sourceTitle).toString(), changedAt = now))
                            }
                            headword = ""; spelling = ""; meaning = ""; source = ""; author = ""; page = ""; quote = ""; partOfSpeech = ""; pronunciation = ""; etymology = ""; sourceUrl = ""; taziehUid = ""; versionUid = ""; roleUid = ""; sectionUid = ""
                            reload(); message = "واژه و معنی به‌صورت پیشنهادی ثبت شد؛ برای استفاده قطعی باید بررسی و تأیید شود."
                        } catch (e: Exception) { message = "ثبت انجام نشد: ${e.message ?: "خطای نامشخص"}" }
                        finally { busy = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "در حال ثبت…" else "ثبت واژه و معنی") }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
            item {
                Text("مدخل‌های ثبت‌شده", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }, label = { Text("همه") })
                    FilterChip(selected = statusFilter == GlossaryTermEntity.STATUS_PENDING, onClick = { statusFilter = GlossaryTermEntity.STATUS_PENDING }, label = { Text("در انتظار بررسی") })
                    FilterChip(selected = statusFilter == GlossaryTermEntity.STATUS_VERIFIED, onClick = { statusFilter = GlossaryTermEntity.STATUS_VERIFIED }, label = { Text("تأییدشده") })
                }
            }
            items(terms, key = { it.id }) { term ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(term.headword, style = MaterialTheme.typography.titleMedium)
                        if (term.partOfSpeech.isNotBlank() || term.pronunciation.isNotBlank()) Text(listOf(term.partOfSpeech, term.pronunciation).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                        if (term.etymology.isNotBlank()) Text("ریشه: ${term.etymology}", style = MaterialTheme.typography.bodySmall)
                        Text("وضعیت: ${if (term.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED) "تأییدشده" else if (term.reviewStatus == GlossaryTermEntity.STATUS_REJECTED) "ردشده" else "در انتظار بررسی"}", style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { scope.launch { selected = term; selectedMeanings = db.glossaryDao().meanings(term.id) } }) { Text("نمایش معنی‌ها") }
                        if (selected?.id == term.id) {
                            selectedMeanings.forEach { m ->
                                Divider()
                                Text(m.meaning)
                                if (m.sourceTitle.isNotBlank()) Text("منبع: ${m.sourceTitle} ${m.sourceAuthor} ${m.sourcePage}", style = MaterialTheme.typography.bodySmall)
                                if (m.sourceUrl.isNotBlank()) Text("پیوند منبع: ${m.sourceUrl}", style = MaterialTheme.typography.bodySmall)
                                val anchors = listOf(m.usageTaziehUid?.let { "تعزیه: $it" }, m.usageVersionUid?.let { "نسخه: $it" }, m.usageRoleUid?.let { "نقش: $it" }, m.usageSectionUid?.let { "بخش: $it" }).filterNotNull()
                                if (anchors.isNotEmpty()) Text(anchors.joinToString(" | "), style = MaterialTheme.typography.bodySmall)
                                if (m.usageQuote.isNotBlank()) Text("شاهد: ${m.usageQuote}", style = MaterialTheme.typography.bodySmall)
                                Text("وضعیت معنی: ${if (m.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED) "تأییدشده" else if (m.reviewStatus == GlossaryTermEntity.STATUS_REJECTED) "ردشده" else "در انتظار بررسی"}", style = MaterialTheme.typography.labelSmall)
                                Row {
                                    TextButton(onClick = { scope.launch { db.withTransaction { val now = System.currentTimeMillis(); db.glossaryDao().updateMeaning(m.copy(reviewStatus = GlossaryTermEntity.STATUS_VERIFIED, updatedAt = now)); db.glossaryDao().insertHistory(GlossaryHistoryEntity(termId = term.id, action = "VERIFY_MEANING", beforeJson = JSONObject().put("status", m.reviewStatus).toString(), afterJson = JSONObject().put("meaningUid", m.uid).put("status", "VERIFIED").toString(), changedAt = now)) }; selectedMeanings = db.glossaryDao().meanings(term.id) } }) { Text("تأیید معنی") }
                                    TextButton(onClick = { scope.launch { db.withTransaction { val now = System.currentTimeMillis(); db.glossaryDao().updateMeaning(m.copy(reviewStatus = GlossaryTermEntity.STATUS_REJECTED, updatedAt = now)); db.glossaryDao().insertHistory(GlossaryHistoryEntity(termId = term.id, action = "REJECT_MEANING", beforeJson = JSONObject().put("status", m.reviewStatus).toString(), afterJson = JSONObject().put("meaningUid", m.uid).put("status", "REJECTED").toString(), changedAt = now)) }; selectedMeanings = db.glossaryDao().meanings(term.id) } }) { Text("رد معنی") }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { scope.launch { db.withTransaction { val before = JSONObject().put("status", term.reviewStatus).toString(); val now = System.currentTimeMillis(); db.glossaryDao().updateTerm(term.copy(reviewStatus = GlossaryTermEntity.STATUS_VERIFIED, updatedAt = now)); db.glossaryDao().insertHistory(GlossaryHistoryEntity(termId = term.id, action = "VERIFY_TERM", beforeJson = before, afterJson = "{\"status\":\"VERIFIED\"}", changedAt = now)) }; reload() } }) { Text("تأیید مدخل") }
                                TextButton(onClick = { scope.launch { db.withTransaction { val now = System.currentTimeMillis(); db.glossaryDao().updateTerm(term.copy(reviewStatus = GlossaryTermEntity.STATUS_REJECTED, updatedAt = now)); db.glossaryDao().insertHistory(GlossaryHistoryEntity(termId = term.id, action = "REJECT_TERM", beforeJson = "{}", afterJson = "{\"status\":\"REJECTED\"}", changedAt = now)) }; reload() } }) { Text("رد مدخل") }
                            }
                        }
                    }
                }
            }
            if (terms.isEmpty()) item { Text("هنوز مدخلی در این فهرست ثبت نشده است.") }
        }
    }
}
