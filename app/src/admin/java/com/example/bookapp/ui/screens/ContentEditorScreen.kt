package com.example.bookapp.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentEditorScreen(
    db: AppDatabase,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var fields by remember { mutableStateOf(emptyList<FieldEntity>()) }
    var taziehs by remember { mutableStateOf(emptyList<TaziehEntity>()) }
    var roles by remember { mutableStateOf(emptyList<RoleEntity>()) }
    var sections by remember { mutableStateOf(emptyList<SectionEntity>()) }
    var footnotes by remember { mutableStateOf(emptyList<FootnoteEntity>()) }
    var images by remember { mutableStateOf(emptyList<TaziehImageEntity>()) }
    var selectedFieldId by remember { mutableLongStateOf(-1L) }
    var selectedTaziehId by remember { mutableLongStateOf(-1L) }
    var selectedRoleId by remember { mutableLongStateOf(-1L) }
    var selectedSectionId by remember { mutableLongStateOf(-1L) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    suspend fun reload() {
        fields = db.fieldDao().getAll()
        taziehs = if (selectedFieldId > 0) db.taziehDao().getByField(selectedFieldId) else emptyList()
        roles = if (selectedTaziehId > 0) db.roleDao().getByTazieh(selectedTaziehId) else emptyList()
        sections = if (selectedRoleId > 0) db.sectionDao().getByRole(selectedRoleId) else emptyList()
        images = if (selectedTaziehId > 0) db.taziehImageDao().getByTazieh(selectedTaziehId) else emptyList()
    }

    LaunchedEffect(Unit) { reload() }
    LaunchedEffect(selectedFieldId) {
        if (selectedFieldId > 0) {
            taziehs = db.taziehDao().getByField(selectedFieldId)
            if (taziehs.none { it.id == selectedTaziehId }) {
                selectedTaziehId = -1L
                selectedRoleId = -1L
                selectedSectionId = -1L
                roles = emptyList()
                sections = emptyList()
                images = emptyList()
                footnotes = emptyList()
            }
        } else {
            taziehs = emptyList()
            selectedTaziehId = -1L
            selectedRoleId = -1L
            selectedSectionId = -1L
            roles = emptyList()
            sections = emptyList()
            images = emptyList()
            footnotes = emptyList()
        }
    }
    LaunchedEffect(selectedTaziehId) {
        if (selectedTaziehId > 0) {
            roles = db.roleDao().getByTazieh(selectedTaziehId)
            images = db.taziehImageDao().getByTazieh(selectedTaziehId)
            if (roles.none { it.id == selectedRoleId }) {
                selectedRoleId = -1L
                selectedSectionId = -1L
                sections = emptyList()
                footnotes = emptyList()
            }
        } else {
            roles = emptyList()
            sections = emptyList()
            images = emptyList()
            footnotes = emptyList()
            selectedRoleId = -1L
            selectedSectionId = -1L
        }
    }
    LaunchedEffect(selectedRoleId) {
        sections = if (selectedRoleId > 0) db.sectionDao().getByRole(selectedRoleId) else emptyList()
        if (sections.none { it.id == selectedSectionId }) selectedSectionId = -1L
        footnotes = emptyList()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("ویرایش محتوای تعزیه") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("مدیریت ساختاری محتوا", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("زمینه، تعزیه، نقش و بخش را مستقیماً ویرایش کنید. حذف هر مورد، روابط وابسته آن را نیز طبق ساختار پایگاه‌داده حذف می‌کند.")
                    }
                }
            }
            item {
                LevelHeader("۱. زمینه‌های تعزیه", Icons.Filled.Folder, "افزودن زمینه") { dialog = EditorDialog.Field(null) }
            }
            items(fields, key = { "f${it.id}" }) { field ->
                EntityRow(field.title, if (field.id == selectedFieldId) "انتخاب‌شده" else null,
                    onClick = { selectedFieldId = field.id },
                    onEdit = { dialog = EditorDialog.Field(field) },
                    onDelete = { deleteTarget = DeleteTarget.Field(field) })
            }
            if (selectedFieldId > 0) {
                item {
                    LevelHeader("۲. تعزیه‌های زمینه «${fields.firstOrNull { it.id == selectedFieldId }?.title ?: ""}»", Icons.Filled.MenuBook, "افزودن تعزیه") {
                        dialog = EditorDialog.Tazieh(null, selectedFieldId)
                    }
                }
                items(taziehs, key = { "t${it.id}" }) { t ->
                    EntityRow(t.title, t.author?.takeIf { it.isNotBlank() },
                        onClick = { selectedTaziehId = t.id },
                        onEdit = { dialog = EditorDialog.Tazieh(t, t.fieldId) },
                        onDelete = { deleteTarget = DeleteTarget.Tazieh(t) })
                }
                item {
                    LevelHeader("تصاویر تعزیه انتخاب‌شده", Icons.Filled.Image, "افزودن تصویر") {
                        if (selectedTaziehId > 0) dialog = EditorDialog.Image(null, selectedTaziehId)
                    }
                }
                items(images, key = { "img${it.id}" }) { image ->
                    EntityRow(image.caption.ifBlank { "تصویر ${image.id}" }, image.filePath, onClick = {},
                        onEdit = { dialog = EditorDialog.Image(image, image.taziehId) },
                        onDelete = { deleteTarget = DeleteTarget.Image(image) })
                }
            }
            if (selectedTaziehId > 0) {
                item {
                    val title = taziehs.firstOrNull { it.id == selectedTaziehId }?.title ?: ""
                    LevelHeader("۳. نقش‌های «$title»", Icons.Filled.Person, "افزودن نقش") {
                        dialog = EditorDialog.Role(null, selectedTaziehId)
                    }
                }
                items(roles, key = { "r${it.id}" }) { r ->
                    EntityRowWithMove(r.title, "ترتیب ${r.orderIndex + 1}",
                        canUp = roles.indexOf(r) > 0, canDown = roles.indexOf(r) < roles.lastIndex,
                        onClick = { selectedRoleId = r.id },
                        onEdit = { dialog = EditorDialog.Role(r, r.taziehId) },
                        onDelete = { deleteTarget = DeleteTarget.Role(r) },
                        onUp = { scope.launch { moveRole(r, roles, db); reload() } },
                        onDown = { scope.launch { moveRoleDown(r, roles, db); reload() } })
                }
            }
            if (selectedRoleId > 0) {
                item {
                    val title = roles.firstOrNull { it.id == selectedRoleId }?.title ?: ""
                    LevelHeader("۴. بخش‌های نقش «$title»", Icons.Filled.MenuBook, "افزودن بخش") {
                        dialog = EditorDialog.Section(null, selectedRoleId)
                    }
                }
                items(sections, key = { "s${it.id}" }) { s ->
                    EntityRowWithMove(
                        s.title.ifBlank { "بخش ${s.orderIndex + 1}" },
                        "ترتیب ${s.orderIndex + 1} • ${s.content.length} نویسه${if (s.audioUrl.isNullOrBlank()) "" else " • صوت دارد"}",
                        canUp = sections.indexOf(s) > 0, canDown = sections.indexOf(s) < sections.lastIndex,
                        onClick = { selectedSectionId = s.id; scope.launch { footnotes = db.footnoteDao().getBySection(s.id) } },
                        onEdit = { dialog = EditorDialog.Section(s, s.roleId) },
                        onDelete = { deleteTarget = DeleteTarget.Section(s) },
                        onUp = { scope.launch { moveSection(s, sections, db); reload() } },
                        onDown = { scope.launch { moveSectionDown(s, sections, db); reload() } })
                }
                item {
                    LevelHeader("۵. پانویس‌های بخش انتخاب‌شده", Icons.Filled.MenuBook, "افزودن پانویس") {
                        if (selectedSectionId > 0) dialog = EditorDialog.Footnote(null, selectedSectionId)
                    }
                }
                items(footnotes, key = { "fn${it.id}" }) { f ->
                    EntityRow(f.term, f.explanation, onClick = {}, onEdit = { dialog = EditorDialog.Footnote(f, f.sectionId) }, onDelete = { deleteTarget = DeleteTarget.Footnote(f) })
                }
            }
            if (message != null) item { Text(message!!, color = MaterialTheme.colorScheme.primary) }
        }
    }

    dialog?.let { d ->
        when (d) {
            is EditorDialog.Field -> FieldDialog(d.value, onDismiss = { dialog = null }) { title ->
                scope.launch { busy = true; try { if (d.value == null) db.fieldDao().insert(FieldEntity(title = title)) else db.fieldDao().updateIdentity(d.value.id, title, d.value.uid); reload(); message = "زمینه ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Tazieh -> TaziehDialog(d.value, fields, d.parentFieldId, { dialog = null }) { title, author ->
                scope.launch { busy = true; try { if (d.value == null) db.taziehDao().insert(TaziehEntity(fieldId = d.parentFieldId, title = title, author = author.ifBlank { null })) else { db.taziehDao().updateIdentity(d.value.id, d.parentFieldId, title, d.value.uid); db.taziehDao().updateAuthor(d.value.id, author.ifBlank { null }, d.value.authorEmail) }; reload(); message = "تعزیه ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Image -> ImageDialog(d.value, { dialog = null }) { path, caption ->
                scope.launch { busy = true; try { if (d.value == null) db.taziehImageDao().insert(TaziehImageEntity(taziehId = d.taziehId, filePath = path, caption = caption)) else db.taziehImageDao().updateCaption(d.value.id, caption); reload(); message = "تصویر ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Role -> RoleDialog(d.value, d.parentTaziehId, { dialog = null }) { title, order ->
                scope.launch { busy = true; try { if (d.value == null) db.roleDao().insert(RoleEntity(taziehId = d.parentTaziehId, title = title, orderIndex = order)) else db.roleDao().updateFromContent(d.value.id, d.parentTaziehId, title, order, d.value.uid); reload(); message = "نقش ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Section -> SectionDialog(d.value, d.parentRoleId, { dialog = null }) { title, content, audio, order ->
                scope.launch { busy = true; try { if (d.value == null) db.sectionDao().insert(SectionEntity(roleId = d.parentRoleId, orderIndex = order, title = title, content = content, audioUrl = audio.ifBlank { null })) else db.sectionDao().updateFromContent(d.value.id, d.parentRoleId, title, content, audio.ifBlank { null }, order, d.value.uid, d.value.sourceUid); reload(); message = "بخش ذخیره شد." } finally { busy = false; dialog = null } }
            }
            is EditorDialog.Footnote -> FootnoteDialog(d.value, d.sectionId, { dialog = null }) { term, explanation ->
                scope.launch { busy = true; try { if (d.value == null) db.footnoteDao().insert(FootnoteEntity(sectionId = d.sectionId, term = term, explanation = explanation)) else db.footnoteDao().update(d.value.copy(sectionId = d.sectionId, term = term, explanation = explanation)); footnotes = db.footnoteDao().getBySection(d.sectionId); message = "پانویس ذخیره شد." } finally { busy = false; dialog = null } }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف محتوا") },
            text = { Text("آیا از حذف «${target.title}» مطمئن هستید؟ موارد وابسته نیز ممکن است حذف شوند.") },
            confirmButton = { Button(onClick = {
                scope.launch { busy = true; try { when (target) { is DeleteTarget.Field -> { TombstoneStore.add(context, "fields", target.value.uid); db.taziehDao().getByField(target.value.id).forEach { t -> TombstoneStore.add(context,"taziehs",t.uid); db.roleDao().getByTazieh(t.id).forEach { r -> TombstoneStore.add(context,"roles",r.uid); db.sectionDao().getByRole(r.id).forEach { sec -> TombstoneStore.add(context,"sections",sec.uid) } } }; db.fieldDao().delete(target.value.id) }; is DeleteTarget.Tazieh -> { TombstoneStore.add(context, "taziehs", target.value.uid); db.roleDao().getByTazieh(target.value.id).forEach { r -> TombstoneStore.add(context,"roles",r.uid); db.sectionDao().getByRole(r.id).forEach { sec -> TombstoneStore.add(context,"sections",sec.uid) } }; db.taziehDao().delete(target.value.id) }; is DeleteTarget.Role -> { TombstoneStore.add(context, "roles", target.value.uid); db.sectionDao().getByRole(target.value.id).forEach { sec -> TombstoneStore.add(context,"sections",sec.uid) }; db.roleDao().delete(target.value.id) }; is DeleteTarget.Section -> { TombstoneStore.add(context, "sections", target.value.uid); db.sectionDao().delete(target.value.id) }; is DeleteTarget.Footnote -> { TombstoneStore.add(context, "footnotes", target.value.uid); db.footnoteDao().delete(target.value.id) }; is DeleteTarget.Image -> { TombstoneStore.add(context, "images", target.value.uid); db.taziehImageDao().delete(target.value.id) } }; selectedFieldId = if (target is DeleteTarget.Field) -1L else selectedFieldId; selectedTaziehId = -1; selectedRoleId = -1; selectedSectionId = -1; footnotes = emptyList(); reload(); message = "مورد حذف شد." } finally { busy = false; deleteTarget = null } }
            }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } }
        )
    }
    if (busy) AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("در حال ذخیره…") }, text = { LinearProgressIndicator(Modifier.fillMaxWidth()) })
}

private sealed interface EditorDialog {
    data class Field(val value: FieldEntity?) : EditorDialog
    data class Tazieh(val value: TaziehEntity?, val parentFieldId: Long) : EditorDialog
    data class Role(val value: RoleEntity?, val parentTaziehId: Long) : EditorDialog
    data class Section(val value: SectionEntity?, val parentRoleId: Long) : EditorDialog
    data class Footnote(val value: FootnoteEntity?, val sectionId: Long) : EditorDialog
    data class Image(val value: TaziehImageEntity?, val taziehId: Long) : EditorDialog
}
private sealed interface DeleteTarget { val title: String; data class Field(val value: FieldEntity): DeleteTarget { override val title get()=value.title }; data class Tazieh(val value:TaziehEntity):DeleteTarget { override val title get()=value.title }; data class Role(val value:RoleEntity):DeleteTarget { override val title get()=value.title }; data class Section(val value:SectionEntity):DeleteTarget { override val title get()=value.title }; data class Footnote(val value:FootnoteEntity):DeleteTarget { override val title get()=value.term }; data class Image(val value:TaziehImageEntity):DeleteTarget { override val title get()=value.caption.ifBlank { value.filePath } } }

private suspend fun moveRole(item: RoleEntity, list: List<RoleEntity>, db: AppDatabase) {
    val i=list.indexOfFirst { it.id==item.id }; if(i>0){ val other=list[i-1]; db.roleDao().setOrder(item.id, other.orderIndex); db.roleDao().setOrder(other.id,item.orderIndex) }
}
private suspend fun moveRoleDown(item: RoleEntity, list: List<RoleEntity>, db: AppDatabase) {
    val i=list.indexOfFirst { it.id==item.id }; if(i>=0 && i<list.lastIndex){ val other=list[i+1]; db.roleDao().setOrder(item.id, other.orderIndex); db.roleDao().setOrder(other.id,item.orderIndex) }
}
private suspend fun moveSection(item: SectionEntity, list: List<SectionEntity>, db: AppDatabase) {
    val i=list.indexOfFirst { it.id==item.id }; if(i>0){ val other=list[i-1]; db.sectionDao().setOrder(item.id, other.orderIndex); db.sectionDao().setOrder(other.id,item.orderIndex) }
}
private suspend fun moveSectionDown(item: SectionEntity, list: List<SectionEntity>, db: AppDatabase) {
    val i=list.indexOfFirst { it.id==item.id }; if(i>=0 && i<list.lastIndex){ val other=list[i+1]; db.sectionDao().setOrder(item.id, other.orderIndex); db.sectionDao().setOrder(other.id,item.orderIndex) }
}

@Composable private fun EntityRowWithMove(title:String, subtitle:String?, canUp:Boolean, canDown:Boolean, onClick:()->Unit, onEdit:()->Unit, onDelete:()->Unit, onUp:()->Unit, onDown:()->Unit) {
    OutlinedCard(onClick=onClick, modifier=Modifier.fillMaxWidth()){ Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement=Arrangement.spacedBy(2.dp)){ Column(Modifier.weight(1f).padding(4.dp)){Text(title,fontWeight=FontWeight.SemiBold); subtitle?.let{Text(it,style=MaterialTheme.typography.bodySmall)}}; Column{IconButton(enabled=canUp,onClick=onUp){Icon(Icons.Filled.KeyboardArrowUp,"بالا")};IconButton(enabled=canDown,onClick=onDown){Icon(Icons.Filled.KeyboardArrowDown,"پایین")}};IconButton(onClick=onEdit){Icon(Icons.Filled.Edit,"ویرایش")};IconButton(onClick=onDelete){Icon(Icons.Filled.Delete,"حذف")}}}
}

@Composable private fun LevelHeader(title:String, icon: androidx.compose.ui.graphics.vector.ImageVector, action:String, onAdd:()->Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Row(Modifier.weight(1f)) { Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(title, fontWeight=FontWeight.Bold) }; TextButton(onClick=onAdd){ Icon(Icons.Filled.Add,null); Spacer(Modifier.width(4.dp)); Text(action) } } }
@Composable private fun EntityRow(title:String, subtitle:String?, onClick:()->Unit, onEdit:()->Unit, onDelete:()->Unit) { OutlinedCard(onClick=onClick, modifier=Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) { Column(Modifier.weight(1f)) { Text(title, fontWeight=FontWeight.SemiBold); subtitle?.let { Text(it, style=MaterialTheme.typography.bodySmall) } }; IconButton(onClick=onEdit){ Icon(Icons.Filled.Edit,"ویرایش") }; IconButton(onClick=onDelete){ Icon(Icons.Filled.Delete,"حذف") } } } }

@Composable private fun FieldDialog(value:FieldEntity?, onDismiss:()->Unit, onSave:(String)->Unit){ SimpleTextDialog(if(value==null)"افزودن زمینه" else "ویرایش زمینه", value?.title.orEmpty(), "نام زمینه", onDismiss,onSave) }
@Composable private fun TaziehDialog(value:TaziehEntity?, fields:List<FieldEntity>, parent:Long, onDismiss:()->Unit, onSave:(String,String)->Unit){ var title by remember{mutableStateOf(value?.title.orEmpty())}; var author by remember{mutableStateOf(value?.author.orEmpty())}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن تعزیه" else "ویرایش تعزیه")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedTextField(title,{title=it},label={Text("نام تعزیه")},singleLine=true); OutlinedTextField(author,{author=it},label={Text("نویسنده")},singleLine=true); Text("زمینه: ${fields.firstOrNull{it.id==parent}?.title.orEmpty()}") }},confirmButton={Button(enabled=title.isNotBlank(),onClick={onSave(title.trim(),author.trim())}){Icon(Icons.Filled.Save,null); Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}}) }
@Composable private fun RoleDialog(value:RoleEntity?, parent:Long, onDismiss:()->Unit, onSave:(String,Int)->Unit){ var title by remember{mutableStateOf(value?.title.orEmpty())}; var order by remember{mutableIntStateOf(value?.orderIndex ?: 0)}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن نقش" else "ویرایش نقش")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it},label={Text("نام نقش")},singleLine=true);OutlinedTextField(order.toString(),{it.toIntOrNull()?.let{n->order=n}},label={Text("شماره ترتیب")},singleLine=true)}},confirmButton={Button(enabled=title.isNotBlank(),onClick={onSave(title.trim(),order)}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}}) }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SectionDialog(
    value: SectionEntity?,
    parent: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf(value?.title.orEmpty()) }
    var editor by remember { mutableStateOf(TextFieldValue(value?.content.orEmpty())) }
    var audio by remember { mutableStateOf(value?.audioUrl.orEmpty()) }
    var order by remember { mutableIntStateOf(value?.orderIndex ?: 0) }
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember { AudioPlayerHelper(context) { state -> isPlaying = state == "started" } }
    DisposableEffect(Unit) { onDispose { player.stop() } }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val path = copyAudioToAppStorage(context, uri)
            if (path != null) { audio = path; status = "فایل صوتی داخل حافظه امن برنامه ذخیره شد." }
            else status = "ذخیره فایل صوتی ناموفق بود."
        }
    }

    fun replaceText(transform: (String) -> String) {
        val text = editor.text
        editor = TextFieldValue(transform(text), TextRange(transform(text).length))
    }
    fun numberedLines(text: String): String {
        var n = 1
        return text.lines().joinToString("\n") { line ->
            val clean = line.replace(Regex("^\\s*\\d+[.)]\\s*"), "").trimEnd()
            if (clean.isBlank()) "" else "${n++}. $clean"
        }
    }
    fun cleanNumbering(text: String) = text.lines().joinToString("\n") { it.replace(Regex("^\\s*\\d+[.)]\\s*"), "") }
    val matchCount = if (search.isBlank()) 0 else Regex(Regex.escape(search.trim()), RegexOption.IGNORE_CASE).findAll(editor.text).count()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (value == null) "افزودن بخش حرفه‌ای" else "ویرایش حرفه‌ای بخش") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان بخش") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { replaceText { "بیت:\n$it" } }, modifier = Modifier.weight(1f)) { Text("افزودن بیت") }
                    OutlinedButton(onClick = { replaceText(::numberedLines) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.FormatListNumbered, null); Spacer(Modifier.width(4.dp)); Text("شماره‌گذاری")
                    }
                    OutlinedButton(onClick = { replaceText(::cleanNumbering) }, modifier = Modifier.weight(1f)) { Text("حذف شماره") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { replaceText { it.replace("  ", " ").replace("\n\n\n", "\n\n").trim() } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.FormatBold, null); Spacer(Modifier.width(4.dp)); Text("پاکسازی متن")
                    }
                    OutlinedButton(onClick = { audioPicker.launch("audio/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(4.dp)); Text("انتخاب صوت")
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("جستجو داخل متن") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (search.isNotBlank()) Text("$matchCount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("متن تعزیه", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${editor.text.lines().count { it.isNotBlank() }} سطر", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(tonalElevation = 1.dp, modifier = Modifier.weight(1f).heightIn(min = 190.dp, max = 300.dp)) {
                        LazyColumn(contentPadding = PaddingValues(8.dp)) {
                            items(editor.text.lines().size) { index ->
                                val line = editor.text.lines()[index]
                                if (line.isNotBlank()) Text("${index + 1}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editor,
                        onValueChange = { editor = it },
                        label = { Text("متن / بیت‌ها / مصراع‌ها") },
                        minLines = 9,
                        modifier = Modifier.weight(5f).heightIn(min = 190.dp, max = 300.dp)
                    )
                }
                Text("قالب پیشنهادی: هر بیت را در دو سطر بنویسید. شماره‌گذاری می‌تواند خودکار انجام شود.", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(order.toString(), { it.toIntOrNull()?.let { n -> order = n } }, label = { Text("شماره ترتیب") }, singleLine = true, modifier = Modifier.width(130.dp))
                    if (audio.isNotBlank()) {
                        IconButton(onClick = { if (isPlaying) player.stop() else player.play(audio) }) {
                            Icon(if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (isPlaying) "توقف" else "پخش")
                        }
                        Text(if (audio.startsWith("/")) "صوت داخلی برنامه" else "صوت آنلاین", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    } else Text("هنوز صوتی انتخاب نشده است.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = editor.text.isNotBlank(), onClick = { onSave(title.trim(), editor.text.trim(), audio.trim(), order) }) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable private fun SimpleTextDialog(title:String, initial:String, label:String,onDismiss:()->Unit,onSave:(String)->Unit){var text by remember{mutableStateOf(initial)};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(text,{text=it},label={Text(label)},singleLine=true)},confirmButton={Button(enabled=text.isNotBlank(),onClick={onSave(text.trim())}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}

@Composable private fun FootnoteDialog(value:FootnoteEntity?, sectionId:Long, onDismiss:()->Unit, onSave:(String,String)->Unit){ var term by remember{mutableStateOf(value?.term.orEmpty())}; var explanation by remember{mutableStateOf(value?.explanation.orEmpty())}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن پانویس" else "ویرایش پانویس")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(term,{term=it},label={Text("عبارت")},singleLine=true);OutlinedTextField(explanation,{explanation=it},label={Text("توضیح")},minLines=4)}},confirmButton={Button(enabled=term.isNotBlank()&&explanation.isNotBlank(),onClick={onSave(term.trim(),explanation.trim())}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}

@Composable private fun ImageDialog(value:TaziehImageEntity?, onDismiss:()->Unit, onSave:(String,String)->Unit){ var path by remember{mutableStateOf(value?.filePath.orEmpty())}; var caption by remember{mutableStateOf(value?.caption.orEmpty())}; AlertDialog(onDismissRequest=onDismiss,title={Text(if(value==null)"افزودن تصویر" else "ویرایش تصویر")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(path,{path=it},label={Text("مسیر یا URI تصویر")},singleLine=true,enabled=value==null);OutlinedTextField(caption,{caption=it},label={Text("عنوان تصویر")},singleLine=true);Text("برای حفظ سازگاری، مسیر/URI فایل ذخیره می‌شود.",style=MaterialTheme.typography.bodySmall)}},confirmButton={Button(enabled=path.isNotBlank(),onClick={onSave(path.trim(),caption.trim())}){Text("ذخیره")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}
