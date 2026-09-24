package com.example.bookapp.ui.screens

/** یکسان‌سازی حروف و فاصله‌ها برای جستجوی قابل‌اعتماد فارسی. */
fun normalizePersianSearch(value: String): String = value
    .replace('ي', 'ی').replace('ى', 'ی')
    .replace('ك', 'ک')
    .replace('ۀ', 'ه').replace('ة', 'ه')
    .replace('ؤ', 'و').replace('إ', 'ا').replace('أ', 'ا')
    .replace('‌', ' ')
    .replace(Regex("[\u064B-\u065F\u0670]"), "")
    .replace(Regex("\\s+"), " ")
    .trim().lowercase()
