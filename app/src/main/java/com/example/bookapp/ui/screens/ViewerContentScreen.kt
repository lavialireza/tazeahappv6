package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.FieldEntity
import com.example.bookapp.data.TaziehEntity
import com.example.bookapp.data.RoleEntity
import com.example.bookapp.data.SectionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerContentScreen(
    db: AppDatabase,
    onOpenSection: (Long) -> Unit,
    onBack: () -> Unit
) {
    var fields by remember { mutableStateOf(emptyList<FieldEntity>()) }
    var taziehs by remember { mutableStateOf(emptyList<TaziehEntity>()) }
    var roles by remember { mutableStateOf(emptyList<RoleEntity>()) }
    var sections by remember { mutableStateOf(emptyList<SectionEntity>()) }

    var fieldId by remember { mutableStateOf<Long?>(null) }
    var taziehId by remember { mutableStateOf<Long?>(null) }
    var roleId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { fields = db.fieldDao().getAll() }
    LaunchedEffect(fieldId) {
        taziehs = fieldId?.let { db.taziehDao().getByField(it) } ?: emptyList()
        if (fieldId == null) { taziehId = null; roleId = null }
    }
    LaunchedEffect(taziehId) {
        roles = taziehId?.let { db.roleDao().getByTazieh(it) } ?: emptyList()
        if (taziehId == null) roleId = null
    }
    LaunchedEffect(roleId) {
        sections = roleId?.let { db.sectionDao().getByRole(it) } ?: emptyList()
    }

    val title = when {
        roleId != null -> roles.firstOrNull { it.id == roleId }?.title ?: "بخش‌ها"
        taziehId != null -> taziehs.firstOrNull { it.id == taziehId }?.title ?: "نقش‌ها"
        fieldId != null -> fields.firstOrNull { it.id == fieldId }?.title ?: "تعزیه‌ها"
        else -> "مطالعه محتوا"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            roleId != null -> roleId = null
                            taziehId != null -> taziehId = null
                            fieldId != null -> fieldId = null
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                roleId != null -> {
                    items(sections, key = { it.id }) { section ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenSection(section.id) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${section.content.length} نویسه",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                taziehId != null -> {
                    items(roles, key = { it.id }) { role ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { roleId = role.id }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(role.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("مشاهده بخش‌های نقش", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                fieldId != null -> {
                    items(taziehs, key = { it.id }) { tazieh ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { taziehId = tazieh.id }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(tazieh.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("مشاهده نقش‌ها", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                else -> {
                    items(fields, key = { it.id }) { field ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { fieldId = field.id }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(field.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("مشاهده تعزیه‌ها", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
