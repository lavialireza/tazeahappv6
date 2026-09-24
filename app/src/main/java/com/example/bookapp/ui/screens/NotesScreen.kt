package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteEntity>,
    onAddNote: (title: String, content: String) -> Unit,
    onUpdateNote: (NoteEntity, title: String, content: String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onBack: () -> Unit,
    canAdd: Boolean = true,
    canEdit: Boolean = true,
    canDelete: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دفتر یادداشت") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAdd) FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "یادداشت جدید")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("هنوز یادداشتی ثبت نکرده‌اید")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canEdit) { editingNote = note }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3
                                )
                            }
                            if (canDelete) IconButton(onClick = { onDeleteNote(note.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NoteEditorDialog(
            title = "یادداشت جدید",
            initialTitle = "",
            initialContent = "",
            confirmText = "ذخیره",
            onDismiss = { showAddDialog = false },
            onConfirm = { newTitle, newContent ->
                if (newTitle.isNotBlank() || newContent.isNotBlank()) {
                    onAddNote(newTitle.ifBlank { "بدون عنوان" }, newContent)
                }
                showAddDialog = false
            }
        )
    }

    editingNote?.let { note ->
        NoteEditorDialog(
            title = "ویرایش یادداشت",
            initialTitle = note.title,
            initialContent = note.content,
            confirmText = "ذخیره تغییرات",
            onDismiss = { editingNote = null },
            onConfirm = { newTitle, newContent ->
                if (newTitle.isNotBlank() || newContent.isNotBlank()) {
                    onUpdateNote(note, newTitle.ifBlank { "بدون عنوان" }, newContent)
                }
                editingNote = null
            }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    title: String,
    initialTitle: String,
    initialContent: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var titleText by remember(initialTitle) { mutableStateOf(initialTitle) }
    var contentText by remember(initialContent) { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("عنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("متن یادداشت") },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(titleText, contentText) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
