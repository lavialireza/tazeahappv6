package com.example.bookapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardPage(val title: String, val description: String)

private val pages = listOf(
    OnboardPage(
        "به تعزیه و شبیه‌خوانی خوش آمدید",
        "کتابخانه‌ای کامل و آفلاین از نسخه‌های تعزیه، دسته‌بندی‌شده بر اساس زمینه، تعزیه، نقش و بخش."
    ),
    OnboardPage(
        "جستجو، علاقه‌مندی و یادداشت",
        "هر متنی را سریع پیدا کنید، مواردی که دوست دارید را نشان کنید و یادداشت‌های شخصی خود را ثبت کنید."
    ),
    OnboardPage(
        "مطالعه راحت‌تر",
        "بین بخش‌های یک نقش با سوایپ جابه‌جا شوید، سایز فونت و حالت شب را از تنظیمات دلخواه خود کنید، یا متن را با صدا بشنوید."
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val item = pages[page]
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(item.title, style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onFinished) { Text("رد شدن") }

                Row {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(
                                    color = if (index == pagerState.currentPage)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                TextButton(onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinished()
                    }
                }) {
                    Text(if (pagerState.currentPage < pages.size - 1) "بعدی" else "شروع")
                }
            }
        }
    }
}
