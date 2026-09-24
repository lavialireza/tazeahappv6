package com.example.bookapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.RoleEntity
import com.example.bookapp.data.SectionEntity

data class CompareSectionItem(val section: SectionEntity, val roleTitle: String)

@Composable
private fun <T> CompareDropdown(label: String, selected: T?, items: List<T>, itemLabel: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.let(itemLabel) ?: "انتخاب کنید", modifier = Modifier.fillMaxWidth())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { onSelect(item); expanded = false })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    taziehTitle: String,
    roles: List<RoleEntity>,
    sections: List<CompareSectionItem>,
    roleSections: Map<Long, List<SectionEntity>>,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf("role") }
    var roleA by remember(roles) { mutableStateOf(roles.getOrNull(0)) }
    var roleB by remember(roles) { mutableStateOf(roles.getOrNull(1)) }
    var sectionA by remember(sections) { mutableStateOf(sections.getOrNull(0)) }
    var sectionB by remember(sections) { mutableStateOf(sections.getOrNull(1)) }
    var compared by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("مقایسه — $taziehTitle") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!compared) {
                Text("نوع مقایسه", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == "role", { mode = "role"; compared = false }, label = { Text("مقایسه نقش‌ها") })
                    FilterChip(mode == "section", { mode = "section"; compared = false }, label = { Text("مقایسه بخش‌ها") })
                }
                Spacer(Modifier.height(12.dp))
                if (mode == "role") {
                    CompareDropdown("نقش اول", roleA, roles, { it.title }) { roleA = it; compared = false }
                    Spacer(Modifier.height(8.dp))
                    CompareDropdown("نقش دوم", roleB, roles, { it.title }) { roleB = it; compared = false }
                } else {
                    CompareDropdown("بخش اول", sectionA, sections, { "${it.roleTitle} ← ${it.section.title}" }) { sectionA = it; compared = false }
                    Spacer(Modifier.height(8.dp))
                    CompareDropdown("بخش دوم", sectionB, sections, { "${it.roleTitle} ← ${it.section.title}" }) { sectionB = it; compared = false }
                }

                Button(
                    onClick = { compared = true },
                    enabled = if (mode == "role") roleA != null && roleB != null && roleA?.id != roleB?.id else sectionA != null && sectionB != null && sectionA?.section?.id != sectionB?.section?.id,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) { Text("نمایش مقایسه") }
                Text("دو مورد را خودتان انتخاب کنید و سپس «نمایش مقایسه» را بزنید.", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { compared = false }) {
                        Text("تغییر انتخاب")
                    }
                }
                if (mode == "role" && roleA != null && roleB != null) {
                    CompareRoleColumns(
                        roleA!!,
                        roleB!!,
                        roleSections[roleA!!.id].orEmpty(),
                        roleSections[roleB!!.id].orEmpty(),
                        Modifier.weight(1f)
                    )
                } else if (mode == "section" && sectionA != null && sectionB != null) {
                    CompareSectionColumns(sectionA!!, sectionB!!, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompareRoleColumns(roleA: RoleEntity, roleB: RoleEntity, sectionsA: List<SectionEntity>, sectionsB: List<SectionEntity>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text(roleA.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            sectionsA.forEach { section ->
                Text(section.title, style = MaterialTheme.typography.titleSmall)
                Text(section.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
        }
        HorizontalDivider(thickness = 2.dp)
        Column(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text(roleB.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            sectionsB.forEach { section ->
                Text(section.title, style = MaterialTheme.typography.titleSmall)
                Text(section.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CompareSectionColumns(a: CompareSectionItem, b: CompareSectionItem, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text(a.roleTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(a.section.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(a.section.content, style = MaterialTheme.typography.bodyMedium)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        Column(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant).verticalScroll(rememberScrollState()).padding(8.dp)) {
            Text(b.roleTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(b.section.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(b.section.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
