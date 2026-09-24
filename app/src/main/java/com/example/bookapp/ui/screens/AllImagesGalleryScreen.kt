package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

data class GalleryImageItem(
    val id: Long,
    val filePath: String,
    val caption: String,
    val taziehTitle: String
)

/**
 * گالری تجمیعی همه‌ی عکس‌های همه‌ی تعزیه‌ها، از منوی اصلی در دسترس.
 * برخلاف گالری داخل هر تعزیه، این صفحه کاملاً فقط-نمایشی است: کاربر عادی
 * فقط عکس‌ها را می‌بیند و نمی‌تواند اضافه یا حذف کند. افزودن/حذف عکس همچنان
 * فقط از همان صفحه‌ی «تصاویر» داخل هر تعزیه (که قبلاً ساخته شده) ممکن است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllImagesGalleryScreen(
    images: List<GalleryImageItem>,
    onBack: () -> Unit
) {
    var previewImage by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<GalleryImageItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("گالری تصاویر") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (images.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    "هنوز عکسی اضافه نشده.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(images, key = { it.id }) { image ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column {
                            AsyncImage(
                                model = image.filePath,
                                contentDescription = image.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .clickable { previewImage = image }
                            )
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    image.taziehTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    image.caption.ifBlank { "بدون توضیح" },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val preview = previewImage
    if (preview != null) {
        Dialog(
            onDismissRequest = { previewImage = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = preview.filePath,
                    contentDescription = preview.caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { previewImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "بستن تصویر",
                        tint = Color.White
                    )
                }
                if (preview.caption.isNotBlank()) {
                    Text(
                        preview.caption,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

}
