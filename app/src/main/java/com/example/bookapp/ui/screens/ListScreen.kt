package com.example.bookapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

data class ListItemData(val id: Long, val title: String, val subtitle: String? = null)

/**
 * صفحه فهرست عمومی به شکل کارت‌های گرافیکی: برای دسته‌بندی‌ها، زمینه‌ها،
 * کتاب‌ها، فصل‌ها و بخش‌ها استفاده می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericListScreen(
    screenTitle: String,
    items: List<ListItemData>,
    onItemClick: (ListItemData) -> Unit,
    onBack: (() -> Unit)? = null,
    floatingAction: (@Composable () -> Unit)? = null,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelect: ((ListItemData) -> Unit)? = null,
    topBarAction: (@Composable () -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null
) {
    Scaffold(
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    }
                },
                actions = { topBarAction?.invoke() }
            )
        },
        floatingActionButton = { floatingAction?.invoke() }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("موردی برای نمایش وجود ندارد")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    ListCard(
                        item = item,
                        selected = item.id in selectedIds,
                        onClick = {
                            if (onToggleSelect != null) onToggleSelect(item) else onItemClick(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListCard(item: ListItemData, selected: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            DecorativePattern(
                modifier = Modifier.matchParentSize(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    if (item.subtitle != null) {
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** یک الگوی هندسی ساده و ملایم (شبیه نقوش اسلیمی) که پشت کارت‌ها کشیده می‌شود. */
@Composable
private fun DecorativePattern(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val step = 28.dp.toPx()
        var y = -step
        while (y < size.height + step) {
            var x = -step
            while (x < size.width + step) {
                drawCircle(
                    color = color,
                    radius = step / 2.2f,
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                )
                x += step
            }
            y += step
        }
    }
}
