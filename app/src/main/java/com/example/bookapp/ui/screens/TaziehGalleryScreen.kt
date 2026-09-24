package com.example.bookapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.File
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

data class TaziehImageItem(
    val id: Long,
    val filePath: String,
    val caption: String
)

/**
 * گالری تصاویر یک تعزیه: عکس‌های قدیمی نسخه‌های خطی، تعزیه‌خوانان معروف و ...
 * کاربر خودش از گالری گوشی عکس اضافه می‌کند؛ توضیح (caption) اختیاری است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaziehGalleryScreen(
    taziehTitle: String,
    images: List<TaziehImageItem>,
    onAddImage: (android.net.Uri) -> Unit,
    onDeleteImage: (TaziehImageItem) -> Unit,
    onUpdateCaption: (TaziehImageItem, String) -> Unit,
    readOnly: Boolean = false,
    canAddImage: Boolean = !readOnly,
    canDeleteImage: Boolean = !readOnly,
    canEditCaption: Boolean = !readOnly,
    canLargePreview: Boolean = true,
    errorMessage: String? = null,
    onBack: () -> Unit
) {
    var editingImage by remember { mutableStateOf<TaziehImageItem?>(null) }
    var captionText by remember { mutableStateOf("") }
    var previewImage by remember { mutableStateOf<TaziehImageItem?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onAddImage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تصاویر: $taziehTitle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAddImage) ExtendedFloatingActionButton(
                text = { Text("افزودن عکس") },
                icon = { Icon(Icons.Filled.AddAPhoto, contentDescription = null) },
                onClick = { pickImageLauncher.launch("image/*") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (images.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(
                    if (readOnly) "هنوز عکسی برای این مجلس ثبت نشده است." else "برای همین مجلس از دکمه «افزودن عکس» در پایین صفحه عکس انتخاب کنید.\nمثلاً عکس نسخه‌ی خطی یا تعزیه‌خوانان.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(images, key = { it.id }) { image ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column {
                            AsyncImage(
                                model = File(image.filePath),
                                contentDescription = image.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .clickable(enabled = canLargePreview) { previewImage = image }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    image.caption.ifBlank { "بدون توضیح" },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = canEditCaption) {
                                            editingImage = image
                                            captionText = image.caption
                                        }
                                )
                                if (canDeleteImage) IconButton(onClick = { onDeleteImage(image) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف عکس")
                                }
                            }
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
                    model = File(preview.filePath),
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

    val current = editingImage
    if (current != null && canEditCaption) {
        AlertDialog(
            onDismissRequest = { editingImage = null },
            title = { Text("توضیح عکس") },
            text = {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("مثلاً «نسخه خطی قرن ۱۳» یا «مرحوم فلانی در نقش شمر»") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateCaption(current, captionText.trim())
                    editingImage = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { editingImage = null }) { Text("انصراف") }
            }
        )
    }
}
