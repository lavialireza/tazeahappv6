package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.TaziehCorrectionStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaziehCorrectionsScreen(
    onBack: () -> Unit,
    canAdd: Boolean = true,
    canEdit: Boolean = true,
    canDelete: Boolean = true,
    canApply: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var items by remember { mutableStateOf(TaziehCorrectionStore.get(context)) }
    var query by remember { mutableStateOf("") }
    var editorItem by remember { mutableStateOf<TaziehCorrectionStore.Correction?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var applyItem by remember { mutableStateOf<TaziehCorrectionStore.Correction?>(null) }
    val filtered = items.filter { query.isBlank() || it.original.contains(query, true) || it.corrected.contains(query, true) || it.explanation.contains(query, true) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("دیکشنری اصلاح و تصحیح متن تعزیه") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } }) },
        floatingActionButton = { if (canAdd) FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "افزودن اصلاح") } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(query, { query = it }, label = { Text("جستجو در اصلاحات") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
            if (!message.isNullOrBlank()) Text(message!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("اصلی: ${item.original}", style = MaterialTheme.typography.titleMedium)
                            Text("اصلاح‌شده: ${item.corrected}", style = MaterialTheme.typography.bodyLarge)
                            if (item.explanation.isNotBlank()) Text(item.explanation, style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                if (canApply) TextButton(onClick = { applyItem = item }) { Text("اعمال بر متن") }
                                if (canEdit) IconButton(onClick = { editorItem = item }) { Icon(Icons.Filled.Edit, "ویرایش") }
                                if (canDelete) IconButton(onClick = { TaziehCorrectionStore.delete(context, item.id); items = TaziehCorrectionStore.get(context) }) { Icon(Icons.Filled.Delete, "حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
    applyItem?.let { item ->
        CorrectionApplyDialog(
            item = item,
            onDismiss = { applyItem = null }
        )
    }

    if (showAdd) CorrectionEditorDialog("افزودن اصلاح", null, { showAdd = false }) { o, c, e ->
        message = if (TaziehCorrectionStore.add(context, o, c, e)) "اصلاح ذخیره شد." else "افزودن اصلاح انجام نشد؛ ممکن است تکراری یا ناقص باشد."
        items = TaziehCorrectionStore.get(context); showAdd = false
    }
    editorItem?.let { item -> CorrectionEditorDialog("ویرایش اصلاح", item, { editorItem = null }) { o, c, e ->
        message = if (TaziehCorrectionStore.update(context, item, o, c, e)) "اصلاح ویرایش و ذخیره شد." else "ویرایش انجام نشد."
        items = TaziehCorrectionStore.get(context); editorItem = null
    } }
}

@Composable
private fun CorrectionApplyDialog(item: TaziehCorrectionStore.Correction, onDismiss: () -> Unit) {
    var source by remember(item) { mutableStateOf("") }
    var result by remember(item) { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("اعمال اصلاح بر متن") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("متن را وارد کنید تا این اصلاح روی آن اعمال شود.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(source, { source = it }, label = { Text("متن اصلی") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            if (result.isNotBlank()) OutlinedTextField(result, {}, label = { Text("نتیجه") }, modifier = Modifier.fillMaxWidth(), minLines = 4, readOnly = true)
        }
    }, confirmButton = {
        TextButton(onClick = { result = source.replace(item.original, item.corrected) }) { Text("اعمال") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } })
}

@Composable
private fun CorrectionEditorDialog(title: String, item: TaziehCorrectionStore.Correction?, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var original by remember(item) { mutableStateOf(item?.original.orEmpty()) }
    var corrected by remember(item) { mutableStateOf(item?.corrected.orEmpty()) }
    var explanation by remember(item) { mutableStateOf(item?.explanation.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(original, { original = it }, label = { Text("عبارت اصلی") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(corrected, { corrected = it }, label = { Text("عبارت اصلاح‌شده") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(explanation, { explanation = it }, label = { Text("توضیح") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { onConfirm(original, corrected, explanation) }) { Text("ذخیره") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
