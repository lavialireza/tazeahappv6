package com.example.bookapp.data

import android.os.Build

data class MuharramEvent(
    val title: String,
    val hijriMonth: Int, // ۰=محرم، ۱=صفر (بر اساس اندیس IslamicCalendar)
    val hijriDay: Int,
    val description: String,
    val matchKeyword: String // برای پیدا کردن تعزیه‌ی مرتبط بر اساس عنوان
)

val MUHARRAM_EVENTS = listOf(
    MuharramEvent("اول محرم", 0, 1, "آغاز ماه محرم و ورود کاروان امام حسین (ع) به کربلا در این حدود.", "ورود"),
    MuharramEvent("تاسوعا", 0, 9, "روز نهم محرم، یک روز مانده به عاشورا.", "تاسوعا"),
    MuharramEvent("عاشورا", 0, 10, "روز دهم محرم، روز شهادت امام حسین (ع) و یاران ایشان در کربلا.", "عاشورا"),
    MuharramEvent("اربعین", 1, 20, "چهلمین روز پس از عاشورا (بیستم صفر)، یادبود شهدای کربلا.", "اربعین")
)

data class MuharramCountdown(
    val event: MuharramEvent,
    val daysRemaining: Long,
    val isToday: Boolean
)

/**
 * چند روز تا هر مناسبت محرم/صفر باقی مانده را محاسبه می‌کند.
 * از android.icu.util.IslamicCalendar استفاده می‌کند (فقط اندروید ۷ به بالا/API 24+
 * در دسترس است)؛ روی نسخه‌های قدیمی‌تر لیست خالی برمی‌گرداند.
 */
fun computeMuharramCountdowns(): List<MuharramCountdown>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
    return try {
        val now = android.icu.util.IslamicCalendar()
        val todayMillis = now.timeInMillis
        val currentHijriYear = now.get(android.icu.util.IslamicCalendar.YEAR)

        MUHARRAM_EVENTS.map { event ->
            fun buildFor(year: Int): android.icu.util.IslamicCalendar {
                val cal = android.icu.util.IslamicCalendar()
                cal.clear()
                cal.set(android.icu.util.IslamicCalendar.YEAR, year)
                cal.set(android.icu.util.IslamicCalendar.MONTH, event.hijriMonth)
                cal.set(android.icu.util.IslamicCalendar.DAY_OF_MONTH, event.hijriDay)
                return cal
            }

            var target = buildFor(currentHijriYear)
            if (target.timeInMillis < todayMillis - 24L * 60 * 60 * 1000) {
                // اگر امسال گذشته، سال هجری بعدی را در نظر بگیر
                target = buildFor(currentHijriYear + 1)
            }
            val diffMillis = target.timeInMillis - todayMillis
            val days = diffMillis / (24L * 60 * 60 * 1000)
            MuharramCountdown(event, days, days == 0L)
        }
    } catch (e: Exception) {
        null
    }
}
