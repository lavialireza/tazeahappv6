package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.bookapp.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceManagementScreen(db: AppDatabase, onBack: () -> Unit, initialTab: Int = 0) {
    val dao = db.performanceDao()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf(emptyList<PerformanceGroupEntity>()) }
    var members by remember { mutableStateOf(emptyList<GroupMemberEntity>()) }
    var performances by remember { mutableStateOf(emptyList<PerformanceEntity>()) }
    var assignments by remember { mutableStateOf(emptyList<PerformanceAssignmentEntity>()) }
    var notes by remember { mutableStateOf(emptyList<PerformanceNoteEntity>()) }
    var groupId by remember { mutableStateOf<Long?>(null) }
    var performanceId by remember { mutableStateOf<Long?>(null) }
    var tab by remember { mutableStateOf(initialTab.coerceIn(0, 5)) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editingGroupId by remember { mutableStateOf<Long?>(null) }; var editingMemberId by remember { mutableStateOf<Long?>(null) }; var editingPerformanceId by remember { mutableStateOf<Long?>(null) }

    var groupName by remember { mutableStateOf("") }; var city by remember { mutableStateOf("") }; var groupNotes by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }; var contact by remember { mutableStateOf("") }; var vocal by remember { mutableStateOf("") }; var memberNotes by remember { mutableStateOf("") }
    var performanceTitle by remember { mutableStateOf("") }; var taziehUid by remember { mutableStateOf("") }; var versionUid by remember { mutableStateOf("") }; var taziehOptions by remember { mutableStateOf(emptyList<TaziehEntity>()) }; var versionOptions by remember { mutableStateOf(emptyList<com.example.bookapp.data.archive.TaziehVersionEntity>()) }; var taziehMenuOpen by remember { mutableStateOf(false) }; var versionMenuOpen by remember { mutableStateOf(false) }; var dateText by remember { mutableStateOf("") }; var venue by remember { mutableStateOf("") }; var performanceNotesText by remember { mutableStateOf("") }
    var roleUid by remember { mutableStateOf("") }; var roleTitle by remember { mutableStateOf("") }; var memberIdText by remember { mutableStateOf("") }; var alternate by remember { mutableStateOf(false) }; var assignmentNotes by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }; var sectionUid by remember { mutableStateOf("") }; var noteType by remember { mutableStateOf("DIRECTOR") }
    var liveAssignmentId by remember { mutableStateOf<Long?>(null) }; var liveSections by remember { mutableStateOf(emptyList<SectionEntity>()) }; var liveRoleWarning by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        groups = dao.groups()
        if (groupId == null || groups.none { it.id == groupId }) groupId = groups.firstOrNull()?.id
        members = groupId?.let { dao.members(it) } ?: emptyList()
        performances = groupId?.let { dao.performances(it) } ?: emptyList()
        if (performanceId == null || performances.none { it.id == performanceId }) performanceId = performances.firstOrNull()?.id
        assignments = performanceId?.let { dao.assignments(it) } ?: emptyList()
        notes = performanceId?.let { dao.notes(it) } ?: emptyList()
    }
    LaunchedEffect(Unit) { taziehOptions = db.taziehDao().getAll(); reload() }
    LaunchedEffect(groupId, performanceId) { reload() }

    Scaffold(topBar = { TopAppBar(title = { Text("مدیریت گروه و اجرای تعزیه") }, navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { Text("مدیریت گروه، اعضا، برنامه اجرا، تخصیص نقش و یادداشت‌های کارگردان", style = MaterialTheme.typography.bodyMedium) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("گروه‌ها", "اعضا", "اجراها", "تخصیص نقش", "یادداشت‌ها", "متن اجرای زنده").forEachIndexed { i, label -> FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) }) } } }
            item { if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary) }
            when (tab) {
                0 -> {
                    item { Text("گروه‌های تعزیه", style = MaterialTheme.typography.titleLarge) }
                    item { OutlinedTextField(groupName, { groupName = it }, Modifier.fillMaxWidth(), label = { Text("نام گروه *") }, singleLine = true) }
                    item { OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text("شهر / منطقه") }, singleLine = true) }
                    item { OutlinedTextField(groupNotes, { groupNotes = it }, Modifier.fillMaxWidth(), label = { Text("توضیحات گروه") }, minLines = 2) }
                    item { Button(enabled = !busy && groupName.isNotBlank(), onClick = { scope.launch { busy = true; try { val now=System.currentTimeMillis(); val editId=editingGroupId; if(editId==null) dao.insertGroup(PerformanceGroupEntity(uid=UUID.randomUUID().toString(), name=groupName.trim(), normalizedName=normalizePerformanceName(groupName), city=city.trim(), notes=groupNotes.trim(), createdAt=now, updatedAt=now)) else { val old=dao.group(editId) ?: error("گروه پیدا نشد"); dao.updateGroup(old.copy(name=groupName.trim(),normalizedName=normalizePerformanceName(groupName),city=city.trim(),notes=groupNotes.trim(),updatedAt=now)) }; editingGroupId=null; groupName=""; city=""; groupNotes=""; reload(); message=if(editId==null) "گروه ثبت شد." else "گروه ویرایش شد." } catch(e:Exception) { message="ذخیره گروه انجام نشد: ${e.message}" } finally { busy=false } } }, modifier=Modifier.fillMaxWidth()) { Text(if(editingGroupId==null) "ثبت گروه" else "ذخیره ویرایش گروه") } }
                    items(groups, key={it.id}) { g -> Card(Modifier.fillMaxWidth().clickable { groupId=g.id; tab=1 }) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement=Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(g.name, style=MaterialTheme.typography.titleMedium); Text("${g.city}  •  شناسه ${g.id}", style=MaterialTheme.typography.bodySmall); if(g.notes.isNotBlank()) Text(g.notes) }; TextButton(onClick={ groupName=g.name; city=g.city; groupNotes=g.notes; editingGroupId=g.id }) { Text("ویرایش") }; TextButton(onClick={scope.launch { dao.deleteGroup(g.id); if(groupId==g.id) groupId=null; if(editingGroupId==g.id) editingGroupId=null; reload(); message="گروه و اطلاعات وابسته آن حذف شد." }}) { Text("حذف") } } } }
                }
                1 -> {
                    item { Text("اعضای گروه", style = MaterialTheme.typography.titleLarge) }
                    item { GroupPicker(groups, groupId) { groupId=it; performanceId=null } }
                    item { OutlinedTextField(memberName, { memberName=it }, Modifier.fillMaxWidth(), label={Text("نام شبیه‌خوان / عضو *")}, singleLine=true) }
                    item { OutlinedTextField(vocal, { vocal=it }, Modifier.fillMaxWidth(), label={Text("گستره یا ویژگی صوتی")}, singleLine=true) }
                    item { OutlinedTextField(contact, { contact=it }, Modifier.fillMaxWidth(), label={Text("راه تماس (اختیاری؛ خصوصی)")}, singleLine=true) }
                    item { OutlinedTextField(memberNotes, { memberNotes=it }, Modifier.fillMaxWidth(), label={Text("توضیحات")}) }
                    item { Button(enabled=groupId!=null && memberName.isNotBlank() && !busy, onClick={scope.launch { busy=true; try { val now=System.currentTimeMillis(); val editId=editingMemberId; if(editId==null) dao.insertMember(GroupMemberEntity(uid=UUID.randomUUID().toString(), groupId=groupId!!, name=memberName.trim(), normalizedName=normalizePerformanceName(memberName), contact=contact.trim(), vocalRange=vocal.trim(), notes=memberNotes.trim(), createdAt=now, updatedAt=now)) else { val old=dao.member(editId) ?: error("عضو پیدا نشد"); require(old.groupId==groupId) { "گروه عضو تغییر کرده است" }; dao.updateMember(old.copy(name=memberName.trim(),normalizedName=normalizePerformanceName(memberName),contact=contact.trim(),vocalRange=vocal.trim(),notes=memberNotes.trim(),updatedAt=now)) }; editingMemberId=null; memberName=""; contact=""; vocal=""; memberNotes=""; reload(); message=if(editId==null) "عضو گروه ثبت شد." else "اطلاعات عضو ویرایش شد." } catch(e:Exception){message="ذخیره عضو انجام نشد: ${e.message}"} finally{busy=false} }}, modifier=Modifier.fillMaxWidth()) { Text(if(editingMemberId==null) "افزودن عضو" else "ذخیره ویرایش عضو") } }
                    items(members, key={it.id}) { m -> ListItem(headlineContent={Text(m.name)}, supportingContent={Text(listOf(m.vocalRange,m.contact,m.notes).filter{it.isNotBlank()}.joinToString(" • "))}, trailingContent={Row { TextButton(onClick={memberName=m.name; contact=m.contact; vocal=m.vocalRange; memberNotes=m.notes; editingMemberId=m.id}) { Text("ویرایش") }; TextButton(onClick={scope.launch { dao.deleteMember(m.id); if(editingMemberId==m.id) editingMemberId=null; reload(); message="عضو حذف شد؛ تخصیص‌های وابسته نیز حذف شدند." }}) { Text("حذف") } }}) }
                }
                2 -> {
                    item { Text("برنامه تمرین و اجرا", style = MaterialTheme.typography.titleLarge) }
                    item { GroupPicker(groups, groupId) { groupId=it; performanceId=null } }
                    item { OutlinedTextField(performanceTitle, {performanceTitle=it}, Modifier.fillMaxWidth(), label={Text("عنوان اجرا / تمرین *")}) }
                    item {
                        Box {
                            OutlinedButton(onClick = { taziehMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(taziehOptions.firstOrNull { it.uid == taziehUid }?.title ?: "انتخاب تعزیه (اختیاری)") }
                            DropdownMenu(expanded = taziehMenuOpen, onDismissRequest = { taziehMenuOpen = false }) {
                                taziehOptions.forEach { item -> DropdownMenuItem(text = { Text(item.title) }, onClick = {
                                    taziehUid = item.uid; versionUid = ""; versionOptions = emptyList(); taziehMenuOpen = false
                                    scope.launch { versionOptions = db.archiveDao().versionsForTazieh(item.uid) }
                                }) }
                            }
                        }
                    }
                    item {
                        Box {
                            OutlinedButton(onClick = { versionMenuOpen = true }, enabled = taziehUid.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(versionOptions.firstOrNull { it.uid == versionUid }?.let { "نسخه ${it.versionNumber}: ${it.title}" } ?: "انتخاب نسخه مشخص برای اجرا") }
                            DropdownMenu(expanded = versionMenuOpen, onDismissRequest = { versionMenuOpen = false }) {
                                versionOptions.forEach { item -> DropdownMenuItem(text = { Text("نسخه ${item.versionNumber}: ${item.title}${if (item.isCurrent) " (جاری)" else ""}") }, onClick = { versionUid = item.uid; versionMenuOpen = false }) }
                            }
                        }
                    }
                    item { OutlinedTextField(dateText, {dateText=it}, Modifier.fillMaxWidth(), label={Text("زمان: YYYY-MM-DD HH:mm (اختیاری)")}, singleLine=true) }
                    item { OutlinedTextField(venue, {venue=it}, Modifier.fillMaxWidth(), label={Text("مکان اجرا")}) }
                    item { OutlinedTextField(performanceNotesText, {performanceNotesText=it}, Modifier.fillMaxWidth(), label={Text("توضیحات برنامه")}) }
                    item { Button(enabled=groupId!=null && performanceTitle.isNotBlank() && !busy, onClick={scope.launch { busy=true; try { val parsed=if(dateText.isBlank()) null else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply{isLenient=false}.parse(dateText)?.time ?: throw IllegalArgumentException("قالب تاریخ صحیح نیست"); val now=System.currentTimeMillis(); val editId=editingPerformanceId; if(editId==null) dao.insertPerformance(PerformanceEntity(uid=UUID.randomUUID().toString(), groupId=groupId!!, title=performanceTitle.trim(), taziehUid=taziehUid.trim(), versionUid=versionUid.trim(), scheduledAt=parsed, venue=venue.trim(), notes=performanceNotesText.trim(), createdAt=now, updatedAt=now)) else { val old=dao.performance(editId) ?: error("برنامه پیدا نشد"); require(old.groupId==groupId) { "گروه برنامه تغییر کرده است" }; dao.updatePerformance(old.copy(title=performanceTitle.trim(),taziehUid=taziehUid.trim(),versionUid=versionUid.trim(),scheduledAt=parsed,venue=venue.trim(),notes=performanceNotesText.trim(),updatedAt=now)) }; editingPerformanceId=null; performanceTitle=""; taziehUid=""; versionUid=""; dateText=""; venue=""; performanceNotesText=""; reload(); message=if(editId==null) "برنامه اجرا ثبت شد." else "برنامه اجرا ویرایش شد." } catch(e:Exception){message="ذخیره برنامه انجام نشد: ${e.message}"} finally{busy=false} }}, modifier=Modifier.fillMaxWidth()) { Text(if(editingPerformanceId==null) "ثبت برنامه" else "ذخیره ویرایش برنامه") } }
                    items(performances, key={it.id}) { p -> Card(Modifier.fillMaxWidth().clickable { performanceId=p.id; tab=3 }) { Column(Modifier.fillMaxWidth().padding(12.dp)) { Text(p.title, style=MaterialTheme.typography.titleMedium); Text("${p.venue} • ${p.status} • شناسه ${p.id}", style=MaterialTheme.typography.bodySmall); if(p.versionUid.isNotBlank()) Text("نسخه متن: ${p.versionUid}", style=MaterialTheme.typography.bodySmall); Row { listOf(PerformanceEntity.STATUS_PLANNED to "برنامه‌ریزی", PerformanceEntity.STATUS_REHEARSAL to "تمرین", PerformanceEntity.STATUS_READY to "آماده اجرا", PerformanceEntity.STATUS_COMPLETED to "انجام‌شده", PerformanceEntity.STATUS_CANCELLED to "لغو").forEach { (v,label) -> TextButton(onClick={scope.launch { dao.updatePerformance(p.copy(status=v,updatedAt=System.currentTimeMillis())); reload() }}) { Text(label) } } }; Row { TextButton(onClick={
                            performanceTitle=p.title; taziehUid=p.taziehUid; versionUid=p.versionUid; dateText=p.scheduledAt?.let { SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date(it)) } ?: ""; venue=p.venue; performanceNotesText=p.notes; editingPerformanceId=p.id
                            scope.launch { versionOptions = if (p.taziehUid.isNotBlank()) db.archiveDao().versionsForTazieh(p.taziehUid) else emptyList() }
                        }) { Text("ویرایش") }; TextButton(onClick={scope.launch { dao.deletePerformance(p.id); if(performanceId==p.id) performanceId=null; if(editingPerformanceId==p.id) editingPerformanceId=null; reload(); message="برنامه و تخصیص‌ها و یادداشت‌های وابسته حذف شدند." }}) { Text("حذف برنامه") } } } } }
                }
                3 -> {
                    item { Text("تخصیص شبیه‌خوان به نقش", style = MaterialTheme.typography.titleLarge) }
                    item { GroupPicker(groups, groupId) { groupId=it; performanceId=null } }
                    item { PerformancePicker(performances, performanceId) { performanceId=it } }
                    item { OutlinedTextField(roleTitle,{roleTitle=it},Modifier.fillMaxWidth(),label={Text("نام نقش *")}) }
                    item { OutlinedTextField(roleUid,{roleUid=it},Modifier.fillMaxWidth(),label={Text("Role UID (در صورت وجود)")}) }
                    item { OutlinedTextField(memberIdText,{memberIdText=it},Modifier.fillMaxWidth(),label={Text("شناسه عضو از فهرست زیر")},singleLine=true) }
                    item { Row { Checkbox(alternate,{alternate=it}); Text("شبیه‌خوان جایگزین") } }
                    item { OutlinedTextField(assignmentNotes,{assignmentNotes=it},Modifier.fillMaxWidth(),label={Text("یادداشت تخصیص")}) }
                    item { Text("اعضای قابل انتخاب:", style=MaterialTheme.typography.titleSmall) }
                    items(members,key={it.id}) { m -> OutlinedCard(Modifier.fillMaxWidth().clickable { memberIdText=m.id.toString() }) { Text("${m.name} — شناسه ${m.id}",Modifier.padding(10.dp)) } }
                    item { Button(enabled=performanceId!=null && roleTitle.isNotBlank() && memberIdText.toLongOrNull()!=null && !busy, onClick={scope.launch { busy=true; try { val member=dao.member(memberIdText.toLong()) ?: error("عضو انتخاب‌شده پیدا نشد"); require(member.groupId==groupId) { "عضو متعلق به گروه انتخاب‌شده نیست" }; dao.insertAssignment(PerformanceAssignmentEntity(performanceId=performanceId!!,memberId=member.id,roleUid=roleUid.trim(),roleTitle=roleTitle.trim(),isAlternate=alternate,notes=assignmentNotes.trim(),assignedAt=System.currentTimeMillis())); roleTitle=""; roleUid=""; memberIdText=""; alternate=false; assignmentNotes=""; reload(); message="تخصیص نقش ثبت شد." } catch(e:Exception){message="تخصیص انجام نشد: ${e.message}"} finally{busy=false} }},modifier=Modifier.fillMaxWidth()) { Text("ثبت تخصیص نقش") } }
                    items(assignments,key={it.id}) { a -> val person=members.firstOrNull{it.id==a.memberId}?.name ?: "عضو ${a.memberId}"; ListItem(headlineContent={Text("${a.roleTitle} — $person")}, supportingContent={Text(if(a.isAlternate) "جایگزین" else "اصلی")}, trailingContent={TextButton(onClick={scope.launch { dao.deleteAssignment(a.id); reload() }}) { Text("حذف") }}) }
                }
                4 -> {
                    item { Text("یادداشت‌های کارگردان و تمرین", style=MaterialTheme.typography.titleLarge) }
                    item { GroupPicker(groups,groupId) { groupId=it; performanceId=null } }
                    item { PerformancePicker(performances,performanceId) { performanceId=it } }
                    item { OutlinedTextField(sectionUid,{sectionUid=it},Modifier.fillMaxWidth(),label={Text("Section UID (اختیاری؛ برای یادداشت روی بخش مشخص)")}) }
                    item { Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { listOf("DIRECTOR" to "کارگردان","REHEARSAL" to "تمرین","CUE" to "علامت ورود/خروج","GENERAL" to "عمومی").forEach{(v,l)->FilterChip(selected=noteType==v,onClick={noteType=v},label={Text(l)})} } }
                    item { OutlinedTextField(noteBody,{noteBody=it},Modifier.fillMaxWidth(),label={Text("متن یادداشت *")},minLines=4) }
                    item { Button(enabled=performanceId!=null && noteBody.isNotBlank() && !busy,onClick={scope.launch { busy=true; try { val now=System.currentTimeMillis(); dao.insertNote(PerformanceNoteEntity(performanceId=performanceId!!,sectionUid=sectionUid.trim(),noteType=noteType,body=noteBody.trim(),createdAt=now,updatedAt=now)); noteBody=""; sectionUid=""; reload(); message="یادداشت ثبت شد." } catch(e:Exception){message="ثبت یادداشت انجام نشد: ${e.message}"} finally{busy=false} }},modifier=Modifier.fillMaxWidth()) { Text("ثبت یادداشت") } }
                    items(notes,key={it.id}) { n -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(n.noteType,style=MaterialTheme.typography.labelMedium); if(n.sectionUid.isNotBlank()) Text("بخش: ${n.sectionUid}",style=MaterialTheme.typography.bodySmall); Text(n.body); TextButton(onClick={scope.launch { dao.deleteNote(n.id); reload() }}) { Text("حذف یادداشت") } } } }
                }
                else -> {
                    item { Text("متن اجرای زنده شبیه‌خوان", style=MaterialTheme.typography.titleLarge) }
                    item { Text("گروه و برنامه را انتخاب کنید؛ سپس نقش تخصیص‌یافته را برای نمایش متن درشت انتخاب کنید.", style=MaterialTheme.typography.bodySmall) }
                    item { GroupPicker(groups,groupId) { groupId=it; performanceId=null; liveAssignmentId=null } }
                    item { PerformancePicker(performances,performanceId) { performanceId=it; liveAssignmentId=null } }
                    item { Text("نقش‌های تخصیص‌یافته", style=MaterialTheme.typography.titleSmall) }
                    item {
                        OutlinedButton(enabled = performanceId != null && !busy, onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val p = dao.performance(performanceId!!) ?: error("برنامه اجرا پیدا نشد.")
                                    val snapshot = PerformanceSnapshotService(db).create(p.uid)
                                    val dir = java.io.File(context.filesDir, "performance_snapshots").apply { mkdirs() }
                                    val target = java.io.File(dir, "${snapshot.performanceUid}-${snapshot.createdAt}.snapshot.json")
                                    val temp = java.io.File(dir, target.name + ".tmp")
                                    java.io.FileOutputStream(temp).use { stream ->
                                        stream.write(snapshot.json.toByteArray(Charsets.UTF_8))
                                        stream.fd.sync()
                                    }
                                    if (target.exists()) target.delete()
                                    require(temp.renameTo(target)) { "انتقال Snapshot به فایل نهایی ناموفق بود." }
                                    val actual = com.example.bookapp.data.ContentUid.sha256(target.readText(Charsets.UTF_8))
                                    require(actual == snapshot.sha256) { "هش Snapshot پس از ذخیره تطبیق ندارد." }
                                    message = "Snapshot ثابت اجرای این برنامه ذخیره شد: ${target.name}"
                                } catch (e: Exception) { message = "ساخت Snapshot انجام نشد: ${e.message}" }
                                finally { busy = false }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("ثبت نسخه ثابت و قابل استناد متن اجرای انتخاب‌شده") }
                    }
                    items(assignments,key={it.id}) { a -> FilterChip(selected=liveAssignmentId==a.id,onClick={
                        liveAssignmentId=a.id
                        scope.launch {
                            liveSections=emptyList(); liveRoleWarning=null
                            val role=if(a.roleUid.isNotBlank()) db.roleDao().getByUid(a.roleUid) else null
                            if(role==null) liveRoleWarning="برای این تخصیص، Role UID معتبر ثبت نشده است؛ متن از روی نام نقش حدس زده نمی‌شود."
                            else {
                                val p=performanceId?.let { dao.performance(it) }
                                if(p!=null && p.versionUid.isNotBlank() && role.versionUid.isNotBlank() && p.versionUid!=role.versionUid) liveRoleWarning="هشدار: نسخه نقش با نسخه تعیین‌شده برای اجرا یکسان نیست. متن فعلی نمایش داده می‌شود؛ تطبیق نسخه‌ای باید بررسی شود."
                                liveSections=db.sectionDao().getByRole(role.id)
                            }
                        }
                    },label={Text("${a.roleTitle}${if(a.isAlternate) " (جایگزین)" else ""}")}) }
                    if(liveRoleWarning!=null) item { Text(liveRoleWarning!!,color=MaterialTheme.colorScheme.error) }
                    val cues=notes.filter { it.noteType=="CUE" }
                    if(cues.isNotEmpty()) item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer)) { Column(Modifier.padding(12.dp)) { Text("علامت‌های ورود / خروج",fontWeight=FontWeight.Bold); cues.forEach { Text(it.body) } } } }
                    items(liveSections,key={it.uid}) { section -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(section.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text(section.content,style=MaterialTheme.typography.bodyLarge.copy(fontSize=26.sp,lineHeight=39.sp)); notes.filter { it.sectionUid.isNotBlank() && it.sectionUid==section.uid }.forEach { n -> Spacer(Modifier.height(8.dp)); Text("یادداشت: ${n.body}",color=MaterialTheme.colorScheme.tertiary) } } } }
                    if(liveAssignmentId!=null && liveSections.isEmpty() && liveRoleWarning==null) item { Text("برای این نقش بخشی پیدا نشد.") }
                    item { Text("توجه: متن نمایش‌داده‌شده از محتوای جاری نقش خوانده می‌شود. اگر اجرای برنامه به نسخه آرشیوی مشخص متصل است، تا تکمیل اتصال کامل متن نسخه‌ای، تطبیق نسخه را کنترل کنید.",style=MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable private fun GroupPicker(groups: List<PerformanceGroupEntity>, selected: Long?, onSelect:(Long)->Unit) { Column { Text("گروه فعال",style=MaterialTheme.typography.labelMedium); Row { groups.forEach { g -> FilterChip(selected=selected==g.id,onClick={onSelect(g.id)},label={Text(g.name)}) } } } }
@Composable private fun PerformancePicker(items: List<PerformanceEntity>, selected: Long?, onSelect:(Long)->Unit) { Column { Text("برنامه فعال",style=MaterialTheme.typography.labelMedium); items.forEach { p -> FilterChip(selected=selected==p.id,onClick={onSelect(p.id)},label={Text(p.title)}) } } }
