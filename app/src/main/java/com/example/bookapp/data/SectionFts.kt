package com.example.bookapp.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * جدول ایندکس متنی (FTS4) روی عنوان و متن بخش‌ها، برای جستجوی سریع به‌جای LIKE.
 * این یک «external content table» است: محتوای واقعی همان جدول sections است،
 * فقط ایندکس جستجو اینجا نگه‌داری می‌شود. همگام‌سازی آن با تریگرهای SQL
 * (در AppDatabase، هنگام ساخت دیتابیس) انجام می‌شود.
 *
 * نکته مهم: tokenizer پیش‌فرض FTS4 («simple») فقط حروف/ارقام لاتین را
 * به‌عنوان کلمه تشخیص می‌دهد و برای متن فارسی/عربی درست کار نمی‌کند (باعث
 * می‌شد جستجوی بیت/متن فارسی نتیجه‌ای پیدا نکند). با tokenizer=unicode61
 * مرز کلمه‌ها در یونیکد (از جمله فارسی) درست تشخیص داده می‌شود.
 */
@Entity(tableName = "sections_fts")
@Fts4(contentEntity = SectionEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class SectionFts(
    val title: String,
    val content: String
)
