package com.example.bookapp.ui.screens


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.bookapp.data.AudioPlayerHelper
import com.example.bookapp.data.ViewerAccessPolicy
import com.example.bookapp.ui.theme.FontChoiceLabels
import com.example.bookapp.ui.theme.FontChoices
import com.example.bookapp.data.FootnoteEntity
import com.example.bookapp.data.Prefs
import com.example.bookapp.data.SearchResult
import com.example.bookapp.data.SpeechHelper
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(
    title: String,
    content: String,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    canBookmark: Boolean = true,
    canBookmarkDelete: Boolean = true,
    canAudio: Boolean = true,
    canTts: Boolean = true,
    canCopy: Boolean = true,
    canShare: Boolean = true,
    sectionId: Long? = null,
    audioUrl: String? = null,
    relatedSections: List<SearchResult> = emptyList(),
    onRelatedClick: (SearchResult) -> Unit = {},
    footnotes: List<FootnoteEntity> = emptyList(),
    saveError: String? = null,
    onAddFootnote: suspend (term: String, explanation: String) -> Result<Unit> = { _, _ -> Result.failure(IllegalStateException("ذخیره پاورقی در دسترس نیست")) },
    onEditFootnote: (FootnoteEntity, term: String, explanation: String) -> Unit = { _, _, _ -> },
    onDeleteFootnote: (FootnoteEntity) -> Unit = {},
    canViewFootnotes: Boolean = true,
    canAddFootnote: Boolean = true,
    canEditFootnote: Boolean = true,
    canDeleteFootnote: Boolean = true,
    canAddFootnoteToDictionary: Boolean = true,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    hasPrevSection: Boolean = false,
    hasNextSection: Boolean = false,
    canNavigate: Boolean = true,
    onPrevSection: () -> Unit = {},
    onNextSection: () -> Unit = {},
    onAttachAudio: (android.net.Uri) -> Unit = {},
    onRemoveAudio: () -> Unit = {},
    fieldTitle: String? = null,
    taziehTitle: String? = null,
    roleTitle: String? = null,
    darkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    fontScale: Float = 1f,
    onFontScaleChange: (Float) -> Unit = {},
    fontChoice: String = "titr",
    onFontChoiceChange: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var immersive by remember { mutableStateOf(false) }

    fun applyImmersive(on: Boolean) {
        val window = activity?.window ?: return
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (on) {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose { applyImmersive(false) } // با خروج از صفحه، نوارهای سیستم برمی‌گردند
    }

    var lineSpacing by remember { mutableFloatStateOf(Prefs.getLineSpacing(context)) }
    // تنظیمات خواننده را محلی هم نگه می‌داریم تا تغییرات داخل همین صفحه
    // بدون وابستگی به بازسازی NavHost فوراً روی متن دیده شوند.
    var readerFontScale by remember { mutableFloatStateOf(fontScale) }
    var readerDarkMode by remember { mutableStateOf(darkMode) }
    var readerFontChoice by remember { mutableStateOf(fontChoice) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                readerFontScale = Prefs.getFontScale(context)
                lineSpacing = Prefs.getLineSpacing(context)
                readerFontChoice = Prefs.getFontChoice(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val scrollState = rememberScrollState()
    val textScope = rememberCoroutineScope()
    LaunchedEffect(fontScale) { readerFontScale = fontScale }
    LaunchedEffect(darkMode) { readerDarkMode = darkMode }
    LaunchedEffect(fontChoice) { readerFontChoice = fontChoice }
    var autoScroll by remember { mutableStateOf(Prefs.isReaderAutoScroll(context)) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val speechHelper = remember {
        SpeechHelper(context) { status ->
            when (status) {
                "no_engine" -> statusMessage = "موتور خواندن صوتی روی این گوشی در دسترس نیست"
                "no_persian_voice" -> statusMessage = "صدای فارسی روی این گوشی نصب نیست (به تنظیمات گوشی مراجعه کنید)"
                "error" -> statusMessage = "خطا در پخش صدا"
            }
            if (status == "done" || status == "error") isSpeaking = false
        }
    }
    val audioPlayerHelper = remember {
        AudioPlayerHelper(context) { status ->
            when (status) {
                "error" -> statusMessage = "خطا در پخش فایل صوتی"
                "done" -> isSpeaking = false
            }
        }
    }
    val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) onAttachAudio(uri)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            isSpeaking = false
            statusMessage = null
        }
    }
    var tag by remember(sectionId) { mutableStateOf(sectionId?.let { Prefs.getTag(context, it) }) }
    var showTagDialog by remember { mutableStateOf(false) }

    // حالت تمام‌صفحه: نوار بالا هنگام اسکرول به پایین جمع می‌شود
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.shutdown()
            audioPlayerHelper.stop()
        }
    }

    // حرکت خودکار فقط برای فایل صوتی واقعی انجام می‌شود؛ چون برای TTS درصد
    // پیشرفت قابل اتکایی در اختیار نداریم. هنگام توقف/پایان صوت، حلقه نیز متوقف می‌شود.
    LaunchedEffect(isSpeaking, audioUrl, autoScroll) {
        if (isSpeaking && autoScroll && !audioUrl.isNullOrBlank()) {
            while (isSpeaking && autoScroll) {
                val progress = audioPlayerHelper.progress()
                val target = (scrollState.maxValue * progress).toInt()
                if (target > 0) {
                    scrollState.animateScrollTo(target)
                }
                kotlinx.coroutines.delay(350)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (readerDarkMode) Color.Black else MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!immersive) {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "جستجو")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                    }
                    if (canAudio || canTts) IconButton(onClick = {
                        if (isSpeaking) {
                            speechHelper.stop()
                            audioPlayerHelper.stop()
                            isSpeaking = false
                        } else {
                            if (canAudio && !audioUrl.isNullOrBlank()) {
                                audioPlayerHelper.play(audioUrl)
                            } else if (canTts) {
                                speechHelper.speak(content)
                            }
                            isSpeaking = true
                        }
                    }) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = if (isSpeaking) "توقف خواندن" else if (!audioUrl.isNullOrBlank()) "پخش صدای واقعی" else "خواندن صوتی"
                        )
                    }
                    if ((isBookmarked && canBookmarkDelete) || (!isBookmarked && canBookmark)) IconButton(onClick = onToggleBookmark) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "نشان کردن"
                        )
                    }
                    var moreExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "بیشتر")
                    }
                    DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (immersive) "خروج از حالت تمام‌صفحه" else "حالت مطالعه بدون مزاحمت (تمام‌صفحه)") },
                            onClick = {
                                moreExpanded = false
                                immersive = !immersive
                                applyImmersive(immersive)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تنظیمات مطالعه متن") },
                            leadingIcon = { Icon(Icons.Filled.TextIncrease, contentDescription = null) },
                            onClick = { moreExpanded = false; showReaderSettings = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (autoScroll) "خاموش کردن حرکت خودکار متن" else "روشن کردن حرکت خودکار متن") },
                            onClick = {
                                moreExpanded = false
                                autoScroll = !autoScroll
                                Prefs.setReaderAutoScroll(context, autoScroll)
                            }
                        )
                        if (sectionId != null) {
                            DropdownMenuItem(
                                text = { Text("برچسب شخصی") },
                                onClick = { moreExpanded = false; showTagDialog = true }
                            )
                        }
                        val copyAllowed = canCopy
                        val shareAllowed = canShare
                        if (copyAllowed) {
                            DropdownMenuItem(
                                text = { Text("کپی متن") },
                                onClick = { moreExpanded = false; copyToClipboard(context, title, content) }
                            )
                        }
                        if (shareAllowed) {
                            DropdownMenuItem(
                                text = { Text("اشتراک‌گذاری") },
                                onClick = { moreExpanded = false; shareText(context, title, content) }
                            )
                            if (sectionId != null) {
                                DropdownMenuItem(
                                    text = { Text("اشتراک‌گذاری لینک مستقیم این بخش") },
                                    onClick = { moreExpanded = false; shareSectionLink(context, title, sectionId) }
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("گزارش اشکال در این متن") },
                            onClick = { moreExpanded = false; reportContentIssue(context, title, content, sectionId) }
                        )
                        if (!com.example.bookapp.BuildConfig.PUBLIC_VIEWER && sectionId != null) {
                            DropdownMenuItem(
                                text = { Text(if (audioUrl.isNullOrBlank()) "افزودن صدای واقعی" else "تعویض صدای واقعی") },
                                onClick = { moreExpanded = false; audioPickerLauncher.launch("audio/*") }
                            )
                            if (!audioUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text("حذف صدای واقعی") },
                                    onClick = { moreExpanded = false; onRemoveAudio() }
                                )
                            }
                        }
                    }
                }
            )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (immersive) Modifier.clickable { immersive = false; applyImmersive(false) } else Modifier)
                .padding(padding)
                .verticalScroll(scrollState),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
        // روی صفحه‌های بزرگ (تبلت) عرض متن محدود می‌شود تا طول خط زیاد نشود و خواندن راحت بماند
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (!immersive && (fieldTitle != null || taziehTitle != null || roleTitle != null)) {
                val breadcrumb = listOfNotNull(fieldTitle, taziehTitle, roleTitle).joinToString(" ← ")
                val verseCount = content.lineSequence().count { it.isNotBlank() }
                Text(
                    breadcrumb,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$verseCount بیت",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!tag.isNullOrBlank()) {
                AssistChip(onClick = { showTagDialog = true }, label = { Text(tag!!) })
                Spacer(Modifier.height(8.dp))
            }
            // تنظیمات محلی صفحه مطالعه مستقیماً از همین state خوانده می‌شوند تا
            // تغییر اندازه و فونت داخل همین پنجره، بدون بستن پنجره، فوراً روی متن دیده شود.
            // سایر بخش‌های برنامه همچنان از MaterialTheme سراسری استفاده می‌کنند.
            val readerFontFamily = FontChoices[readerFontChoice] ?: FontChoices["titr"]!!
            val readerBaseFontSize = 18f * readerFontScale.coerceIn(0.8f, 2.0f)
            val readerLineHeight = readerBaseFontSize * lineSpacing
            var selectedFootnote by remember(footnotes) { mutableStateOf<FootnoteEntity?>(null) }
            val annotatedContent = remember(content, footnotes) {
                buildAnnotatedString {
                    append(content)
                    footnotes.forEachIndexed { index, fn ->
                        var start = 0
                        while (start < length) {
                            val found = content.indexOf(fn.term, start, ignoreCase = false)
                            if (found < 0) break
                            addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), found, found + fn.term.length)
                            addStringAnnotation("FOOTNOTE", index.toString(), found, found + fn.term.length)
                            start = found + fn.term.length
                        }
                    }
                }
            }
            ClickableText(
                text = annotatedContent,
                onClick = { offset ->
                    annotatedContent.getStringAnnotations("FOOTNOTE", offset, offset).firstOrNull()?.let { ann ->
                        selectedFootnote = footnotes.getOrNull(ann.item.toIntOrNull() ?: -1)
                    }
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (readerDarkMode) Color(0xFFEFE0C0) else MaterialTheme.colorScheme.onSurface,
                    fontFamily = readerFontFamily,
                    fontSize = readerBaseFontSize.sp,
                    lineHeight = readerLineHeight.sp
                )
            )
            if (selectedFootnote != null) {
                val fn = selectedFootnote!!
                AlertDialog(
                    onDismissRequest = { selectedFootnote = null },
                    title = { Text("پاورقی") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("واژه", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(fn.term, fontWeight = FontWeight.Bold)
                            Text("توضیح", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(fn.explanation)
                        }
                    },
                    confirmButton = { TextButton(onClick = { selectedFootnote = null }) { Text("بستن") } }
                )
            }

            if (canNavigate && (hasPrevSection || hasNextSection)) {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onPrevSection, enabled = hasPrevSection) {
                        Text("◀ بخش قبل")
                    }
                    OutlinedButton(onClick = onNextSection, enabled = hasNextSection) {
                        Text("بخش بعد ▶")
                    }
                }
            }

            if (sectionId != null && canViewFootnotes) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                FootnotesSection(
                    footnotes = footnotes,
                    onAdd = onAddFootnote,
                    onEdit = onEditFootnote,
                    onDelete = onDeleteFootnote,
                    canAddFootnote = canAddFootnote,
                    canEditFootnote = canEditFootnote,
                    canDeleteFootnote = canDeleteFootnote,
                    onAddToDictionary = { fn ->
                        if (!canAddFootnoteToDictionary) {
                            statusMessage = "انتقال پاورقی به دیکشنری در سیاست دسترسی فعلی غیرفعال است."
                            false
                        } else {
                            val added = com.example.bookapp.data.GlossaryStore.add(context, fn.term, fn.explanation)
                            statusMessage = if (added) "«${fn.term}» به فرهنگ لغت اضافه شد." else "این واژه قبلاً در فرهنگ لغت وجود دارد."
                            added
                        }
                    },
                    canAddFootnoteToDictionary = canAddFootnoteToDictionary,
                    onJumpToText = { fn ->
                        val idx = content.indexOf(fn.term)
                        if (idx >= 0 && content.isNotBlank() && scrollState.maxValue > 0) {
                            val ratio = idx.toFloat() / content.length.coerceAtLeast(1)
                            textScope.launch { scrollState.animateScrollTo((scrollState.maxValue * ratio).toInt().coerceIn(0, scrollState.maxValue)) }
                        }
                    },
                    saveError = saveError
                )
            }

            if (relatedSections.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text("بخش‌های مرتبط (هم‌نام)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                relatedSections.forEach { related ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onRelatedClick(related) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(related.roleTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${related.taziehTitle} · ${related.fieldTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showReaderSettings) {
        AlertDialog(
            onDismissRequest = { showReaderSettings = false },
            title = { Text("تنظیمات مطالعه") },
            text = {
                Column {
                    Text("اندازه متن: ${(readerFontScale * 100).toInt()}٪")
                    Slider(
                        value = readerFontScale,
                        onValueChange = { value ->
                            readerFontScale = value
                            onFontScaleChange(value)
                        },
                        valueRange = 0.8f..2.0f,
                        steps = 11
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("حالت مطالعه شب")
                        Switch(
                            checked = readerDarkMode,
                            onCheckedChange = { enabled ->
                                readerDarkMode = enabled
                                onDarkModeChange(enabled)
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("فاصله خطوط متن")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "فشرده" to 1.1f,
                            "معمولی" to 1.4f,
                            "بازتر" to 1.8f
                        ).forEach { (label, value) ->
                            FilterChip(
                                selected = kotlin.math.abs(lineSpacing - value) < 0.01f,
                                onClick = {
                                    lineSpacing = value
                                    Prefs.setLineSpacing(context, value)
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("فونت متن")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontChoiceLabels.forEach { (key, label) ->
                            FilterChip(
                                selected = readerFontChoice == key,
                                onClick = {
                                    readerFontChoice = key
                                    onFontChoiceChange(key)
                                },
                                label = { Text(label, fontFamily = FontChoices[key]) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("حرکت خودکار متن هنگام پخش صوت")
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = {
                                autoScroll = it
                                Prefs.setReaderAutoScroll(context, it)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReaderSettings = false }) { Text("بستن") }
            }
        )
    }

    if (showTagDialog && sectionId != null) {
        var input by remember { mutableStateOf(tag ?: "") }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("برچسب شخصی") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("مثلاً: حفظ کنم، برای مجلس بعدی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.setTag(context, sectionId, input)
                    tag = input.ifBlank { null }
                    showTagDialog = false
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("انصراف") }
            }
        )
    }
}

private fun copyToClipboard(context: Context, title: String, content: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(title, content)
    clipboard.setPrimaryClip(clip)
}

private fun shareText(context: Context, title: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
}

private fun shareSectionLink(context: Context, title: String, sectionId: Long) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "«$title» را در اپلیکیشن تعزیه ببینید:\ntaziehapp://section/$sectionId")
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری لینک"))
}

/**
 * یک ایمیل با متن بخش و شناسه‌اش آماده می‌کند تا کاربر بتواند غلط تایپی یا
 * اشکال احتمالی حاصل از تبدیل Word→JSON را به سازنده‌ی برنامه گزارش دهد.
 * جای‌گذارنده‌ی [ایمیل خودتان را اینجا بنویسید] باید با ایمیل واقعی جایگزین شود.
 */
private fun reportContentIssue(context: Context, title: String, content: String, sectionId: Long?) {
    val body = buildString {
        appendLine("توضیح اشکال (لطفاً اینجا بنویسید):")
        appendLine()
        appendLine("——————————")
        appendLine("عنوان بخش: $title")
        if (sectionId != null) appendLine("شناسه بخش: $sectionId")
        appendLine("متن فعلی:")
        appendLine(content)
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("your-email@example.com"))
        putExtra(Intent.EXTRA_SUBJECT, "گزارش اشکال محتوا: $title")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // اگر هیچ اپ ایمیلی نصب نبود، به‌جایش اشتراک‌گذاری عمومی نشان می‌دهیم
        shareText(context, "گزارش اشکال: $title", body)
    }
}

/**
 * پاورقی: توضیح واژه‌ها/عبارت‌های یک بخش (معنی لغت، توضیح مختصر، منبع و ...).
 * کاملاً توسط خود کاربر نوشته، ذخیره و ویرایش می‌شود؛ چیزی از پیش تولید نمی‌شود.
 */
private fun normalizeDictionaryTerm(value: String): String = value
    .trim()
    .replace('ي', 'ی')
    .replace('ك', 'ک')
    .replace('ۀ', 'ه')
    .replace('ة', 'ه')
    .replace(Regex("\\s+"), " ")

private fun isAlreadyInDictionary(context: Context, term: String): Boolean {
    val normalized = normalizeDictionaryTerm(term)
    return normalized.isNotEmpty() && (GLOSSARY_TERMS.any {
        normalizeDictionaryTerm(it.term) == normalized
    } || com.example.bookapp.data.GlossaryStore.contains(context, normalized))
}

@Composable
private fun FootnotesSection(
    footnotes: List<FootnoteEntity>,
    onAdd: suspend (term: String, explanation: String) -> Result<Unit>,
    onEdit: (FootnoteEntity, term: String, explanation: String) -> Unit,
    onDelete: (FootnoteEntity) -> Unit,
    onAddToDictionary: (FootnoteEntity) -> Boolean = { false },
    canAddFootnote: Boolean = true,
    canEditFootnote: Boolean = true,
    canDeleteFootnote: Boolean = true,
    canAddFootnoteToDictionary: Boolean = true,
    onJumpToText: (FootnoteEntity) -> Unit = {},
    saveError: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FootnoteEntity?>(null) }
    var saving by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val footnoteScope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text("پاورقی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (canAddFootnote) TextButton(onClick = { editing = null; showDialog = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("افزودن پاورقی")
        }
    }

    if (footnotes.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("جستجوی پاورقی") },
            placeholder = { Text("جستجو در واژه و توضیح...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "جستجو") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
    }

    val normalizedSearch = normalizeDictionaryTerm(searchQuery)
    val visibleFootnotes = if (normalizedSearch.isBlank()) footnotes else footnotes.filter {
        normalizeDictionaryTerm(it.term).contains(normalizedSearch) ||
            normalizeDictionaryTerm(it.explanation).contains(normalizedSearch)
    }

    if (footnotes.isEmpty()) {
        Text(
            "هنوز پاورقی‌ای برای این بخش ثبت نشده (مثلاً معنی یک واژه، توضیح مختصر یا منبع).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else if (visibleFootnotes.isEmpty()) {
        Text(
            "برای «$searchQuery» پاورقی‌ای پیدا نشد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Text(
            "${visibleFootnotes.size} پاورقی${if (normalizedSearch.isNotBlank()) " پیدا شد" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        visibleFootnotes.forEach { fn ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text("واژه", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(2.dp))
                                Text(fn.term, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text("توضیح", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(2.dp))
                                Text(fn.explanation, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (canEditFootnote) IconButton(onClick = { editing = fn; showDialog = true }) {
                        Icon(Icons.Filled.Label, contentDescription = "ویرایش پاورقی")
                    }
                    Column {
                        IconButton(onClick = { onJumpToText(fn) }) {
                            Icon(Icons.Filled.Search, contentDescription = "رفتن به واژه در متن")
                        }
                        if (canAddFootnoteToDictionary) {
                            IconButton(onClick = {
                                onAddToDictionary(fn)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "افزودن به فرهنگ لغت")
                            }
                        }
                        if (canDeleteFootnote) IconButton(onClick = { onDelete(fn) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف پاورقی")
                        }
                    }
                }
            }
        }
    }

    if (saveError != null) {
        Spacer(Modifier.height(6.dp))
        Text(saveError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }

    if (showDialog) {
        var term by remember { mutableStateOf(editing?.term ?: "") }
        var explanation by remember { mutableStateOf(editing?.explanation ?: "") }
        var validationError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "افزودن پاورقی" else "ویرایش پاورقی") },
            text = {
                Column {
                    OutlinedTextField(
                        value = term,
                        onValueChange = { term = it; validationError = null },
                        label = { Text("واژه یا عبارت") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it; validationError = null },
                        label = { Text("توضیح (معنی، منبع، نکته و ...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (validationError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    if (term.isBlank()) {
                        validationError = "واژه یا عبارت را وارد کنید."
                    } else if (explanation.isBlank()) {
                        validationError = "توضیح پاورقی را وارد کنید."
                    } else {
                        val cleanTerm = normalizeDictionaryTerm(term)
                        val current = editing
                        if (current == null && isAlreadyInDictionary(context, cleanTerm)) {
                            validationError = "این واژه قبلاً در دیکشنری وجود دارد و دوباره ثبت نمی‌شود."
                        } else if (current == null) {
                            if (saving) return@TextButton
                            saving = true
                            footnoteScope.launch {
                                val result = runCatching { onAdd(term.trim(), explanation.trim()) }.getOrElse { Result.failure(it) }
                                saving = false
                                result.onSuccess {
                                    showDialog = false
                                }.onFailure {
                                    validationError = "ذخیره پاورقی انجام نشد: ${it.message ?: "خطای نامشخص"}"
                                }
                            }
                        } else if (!canEditFootnote) {
                            validationError = "ویرایش پاورقی در سیاست دسترسی فعلی غیرفعال است."
                        } else {
                            onEdit(current, term.trim(), explanation.trim())
                            showDialog = false
                        }
                    }
                }) { Text(if (saving) "در حال ذخیره…" else "ذخیره") }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showDialog = false }) { Text("انصراف") }
            }
        )
    }
}

/**
 * حالت مطالعه حرفه‌ای: امکان سوایپ (کشیدن انگشت) بین بخش‌های یک نقش،
 * بدون نیاز به برگشتن به فهرست بعد از هر بخش.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TextPagerScreen(
    sections: List<com.example.bookapp.data.SectionEntity>,
    startIndex: Int,
    isBookmarked: (Long) -> Boolean,
    onToggleBookmark: (Long) -> Unit,
    onPageShown: (Long) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onAttachAudio: (sectionId: Long, uri: android.net.Uri) -> Unit = { _, _ -> },
    onRemoveAudio: (sectionId: Long) -> Unit = {},
    footnotesForSection: suspend (Long) -> List<com.example.bookapp.data.FootnoteEntity> = { emptyList() },
    onAddFootnote: suspend (Long, String, String) -> Result<Unit> = { _, _, _ -> Result.failure(IllegalStateException("ذخیره پاورقی در دسترس نیست")) },
    onEditFootnote: (Long, com.example.bookapp.data.FootnoteEntity, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteFootnote: (Long, com.example.bookapp.data.FootnoteEntity) -> Unit = { _, _ -> },
    canViewFootnotes: Boolean = true,
    canAddFootnote: Boolean = true,
    canEditFootnote: Boolean = true,
    canDeleteFootnote: Boolean = true,
    canAddFootnoteToDictionary: Boolean = true,
    canBookmark: Boolean = true,
    canBookmarkDelete: Boolean = true,
    canAudio: Boolean = true,
    canTts: Boolean = true,
    canCopy: Boolean = true,
    canShare: Boolean = true,
    canNavigate: Boolean = true,
    fieldTitle: String? = null,
    taziehTitle: String? = null,
    roleTitle: String? = null,
    onBack: () -> Unit
) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = startIndex.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
    ) { sections.size }
    val pagerScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        if (sections.isNotEmpty()) {
            onPageShown(sections[pagerState.currentPage].id)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (sections.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1).toFloat() / sections.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
        val section = sections[page]
        var pageFootnotes by remember(section.id) { mutableStateOf(listOf<com.example.bookapp.data.FootnoteEntity>()) }
        LaunchedEffect(section.id) { pageFootnotes = footnotesForSection(section.id) }
        Column(Modifier.fillMaxSize()) {
            TextScreen(
                title = "${section.title}  (${page + 1}/${sections.size})",
                content = section.content,
                isBookmarked = isBookmarked(section.id),
                onToggleBookmark = { if ((isBookmarked(section.id) && canBookmarkDelete) || (!isBookmarked(section.id) && canBookmark)) onToggleBookmark(section.id) },
                sectionId = section.id,
                audioUrl = section.audioUrl,
                onOpenSearch = onOpenSearch,
                onOpenSettings = onOpenSettings,
                hasPrevSection = page > 0,
                hasNextSection = page < sections.size - 1,
                canNavigate = canNavigate,
                onPrevSection = {
                    pagerScope.launch { pagerState.animateScrollToPage(page - 1) }
                },
                onNextSection = {
                    pagerScope.launch { pagerState.animateScrollToPage(page + 1) }
                },
                onAttachAudio = { uri -> onAttachAudio(section.id, uri) },
                onRemoveAudio = { onRemoveAudio(section.id) },
                footnotes = pageFootnotes,
                onAddFootnote = { term, explanation ->
                    val result = onAddFootnote(section.id, term, explanation)
                    if (result.isSuccess) pageFootnotes = footnotesForSection(section.id)
                    result
                },
                onEditFootnote = { fn, term, explanation ->
                    onEditFootnote(section.id, fn, term, explanation)
                    pagerScope.launch { pageFootnotes = footnotesForSection(section.id) }
                },
                onDeleteFootnote = { fn ->
                    onDeleteFootnote(section.id, fn)
                    pagerScope.launch { pageFootnotes = footnotesForSection(section.id) }
                },
                canViewFootnotes = canViewFootnotes,
                canAddFootnote = canAddFootnote,
                canEditFootnote = canEditFootnote,
                canDeleteFootnote = canDeleteFootnote,
                canAddFootnoteToDictionary = canAddFootnoteToDictionary,
                canBookmark = canBookmark,
                canBookmarkDelete = canBookmarkDelete,
                canAudio = canAudio,
                canTts = canTts,
                canCopy = canCopy,
                canShare = canShare,
                fieldTitle = fieldTitle,
                taziehTitle = taziehTitle,
                roleTitle = roleTitle,
                onBack = onBack
            )
        }
        }
    }
}
