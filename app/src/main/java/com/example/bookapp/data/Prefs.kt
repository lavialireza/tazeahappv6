package com.example.bookapp.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * ذخیره‌سازی ساده تنظیمات کاربر (حالت روز/شب و سایز فونت) با SharedPreferences.
 */
object Prefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_FONT_SCALE = "font_scale" // 0.85f=کوچک, 1.0f=متوسط, 1.3f=بزرگ

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun getFontScale(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_FONT_SCALE, 1.0f)
    }

    fun setFontScale(context: Context, scale: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    private const val KEY_AUTO_SCROLL_READER = "auto_scroll_reader"

    fun isReaderAutoScroll(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_SCROLL_READER, false)
    }

    fun setReaderAutoScroll(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_SCROLL_READER, enabled).apply()
    }

    private const val KEY_BOOKMARKS = "bookmarks"

    fun getBookmarks(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BOOKMARKS, emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    fun isBookmarked(context: Context, sectionId: Long): Boolean {
        return getBookmarks(context).contains(sectionId)
    }

    fun toggleBookmark(context: Context, sectionId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getBookmarks(context).toMutableSet()
        val nowBookmarked: Boolean
        if (current.contains(sectionId)) {
            current.remove(sectionId)
            nowBookmarked = false
        } else {
            current.add(sectionId)
            nowBookmarked = true
        }
        prefs.edit().putStringSet(KEY_BOOKMARKS, current.map { it.toString() }.toSet()).apply()
        return nowBookmarked
    }

    private const val KEY_RECENT = "recent_sections"
    private const val MAX_RECENT = 10

    /** لیست شناسه‌های اخیراً مشاهده‌شده را از جدید به قدیم برمی‌گرداند */
    fun getRecent(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun addRecent(context: Context, sectionId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getRecent(context).toMutableList()
        current.remove(sectionId)
        current.add(0, sectionId)
        val trimmed = current.take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, trimmed.joinToString(",")).apply()
    }

    private const val KEY_THEME = "theme_choice"

    /** یکی از سه مقدار: "default"، "green"، "red" */
    fun getThemeChoice(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "default") ?: "default"
    }

    fun setThemeChoice(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

    fun isOnboardingShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)
    }

    fun setOnboardingShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
    }

    private const val KEY_FONT_CHOICE = "font_choice"

    /** یکی از مقادیر "titr"، "serif"، "sans"، "cursive" */
    fun getFontChoice(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FONT_CHOICE, "titr") ?: "titr"
    }

    fun setFontChoice(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FONT_CHOICE, value).apply()
    }

    private const val KEY_READ_SECTIONS = "read_sections"
    private const val KEY_LAST_READ_DAY = "last_read_day"
    private const val KEY_STREAK_DAYS = "streak_days"
    private const val KEY_ACTIVE_DAYS = "active_days"

    /** ثبت اینکه یک بخش خوانده شده (برای آمار تعداد کل)، و به‌روزرسانی روزهای متوالی مطالعه */
    fun markSectionRead(context: Context, sectionId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_READ_SECTIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(sectionId.toString())
        prefs.edit().putStringSet(KEY_READ_SECTIONS, current).apply()

        val today = localDayKey() // روز محلی دستگاه
        val lastDay = prefs.getInt(KEY_LAST_READ_DAY, -1)
        val streak = prefs.getInt(KEY_STREAK_DAYS, 0)
        when {
            lastDay == today -> { /* همان روز، تغییری لازم نیست */ }
            lastDay == previousLocalDayKey() -> prefs.edit().putInt(KEY_STREAK_DAYS, streak + 1).putInt(KEY_LAST_READ_DAY, today).apply()
            else -> prefs.edit().putInt(KEY_STREAK_DAYS, 1).putInt(KEY_LAST_READ_DAY, today).apply()
        }

        val activeDays = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        activeDays.add(today.toString())
        prefs.edit().putStringSet(KEY_ACTIVE_DAYS, activeDays).apply()
    }

    /** فعال‌بودن مطالعه در N روز اخیر (برای نمودار ساده)؛ آخرین عنصر لیست همان امروز است */
    fun getActiveDaysLast(context: Context, days: Int = 14): List<Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeDays = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return (days - 1 downTo 0).map { offset ->
            val day = cal.clone() as java.util.Calendar
            day.add(java.util.Calendar.DAY_OF_MONTH, -offset)
            localDayKeyFrom(day).toString() in activeDays
        }
    }

    fun getReadSectionsCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_READ_SECTIONS, emptySet())?.size ?: 0
    }

    fun getStreakDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = localDayKey()
        val lastDay = prefs.getInt(KEY_LAST_READ_DAY, -1)
        // اگر بیش از یک روز از آخرین مطالعه گذشته، زنجیره شکسته است
        return if (lastDay == today || lastDay == previousLocalDayKey()) prefs.getInt(KEY_STREAK_DAYS, 0) else 0
    }

    fun getReadSectionIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_READ_SECTIONS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun mergeReadSectionIds(context: Context, ids: Set<Long>) {
        if (ids.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val merged = prefs.getStringSet(KEY_READ_SECTIONS, emptySet()).orEmpty().toMutableSet()
        merged.addAll(ids.map(Long::toString))
        prefs.edit().putStringSet(KEY_READ_SECTIONS, merged).apply()
    }

    fun getActiveDayValues(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()).orEmpty()
    }

    fun mergeActiveDayValues(context: Context, values: Set<String>) {
        if (values.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val merged = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()).orEmpty().toMutableSet()
        merged.addAll(values)
        prefs.edit().putStringSet(KEY_ACTIVE_DAYS, merged).apply()
    }

    fun getAllTags(context: Context): Map<Long, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(KEY_TAG_PREFIX)) return@mapNotNull null
            val id = key.removePrefix(KEY_TAG_PREFIX).toLongOrNull() ?: return@mapNotNull null
            val tag = value as? String ?: return@mapNotNull null
            id to tag
        }.toMap()
    }

    private const val KEY_TAG_PREFIX = "tag_"

    /** برچسب شخصی یک بخش (مثلاً «حفظ کنم» یا «برای مجلس بعدی») را برمی‌گرداند، یا null اگر برچسبی نداشته باشد */
    fun getTag(context: Context, sectionId: Long): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("$KEY_TAG_PREFIX$sectionId", null)?.takeIf { it.isNotBlank() }
    }

    fun setTag(context: Context, sectionId: Long, tag: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (tag.isBlank()) {
            prefs.edit().remove("$KEY_TAG_PREFIX$sectionId").apply()
        } else {
            prefs.edit().putString("$KEY_TAG_PREFIX$sectionId", tag.trim()).apply()
        }
    }

    private const val KEY_MY_ROLE_PREFIX = "my_role_"

    /** شناسه نقشی که کاربر به‌عنوان «نقش من» برای یک تعزیه انتخاب کرده، یا null */
    fun getMyRole(context: Context, taziehId: Long): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getLong("$KEY_MY_ROLE_PREFIX$taziehId", -1L)
        return if (value == -1L) null else value
    }

    fun setMyRole(context: Context, taziehId: Long, roleId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("$KEY_MY_ROLE_PREFIX$taziehId", roleId).apply()
    }

    fun clearMyRole(context: Context, taziehId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_MY_ROLE_PREFIX$taziehId").apply()
    }

    /** همه‌ی نقش‌های «من» ثبت‌شده روی این گوشی: نگاشت شناسه‌تعزیه به شناسه‌نقش */
    fun getAllMyRoles(context: Context): Map<Long, Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all
            .filterKeys { it.startsWith(KEY_MY_ROLE_PREFIX) }
            .mapNotNull { (key, value) ->
                val taziehId = key.removePrefix(KEY_MY_ROLE_PREFIX).toLongOrNull()
                val roleId = value as? Long
                if (taziehId != null && roleId != null) taziehId to roleId else null
            }
            .toMap()
    }

    private const val KEY_APP_PASSWORD = "app_password"
    private const val PASSWORD_PREFIX = "v2$"
    private const val PASSWORD_ITERATIONS = 210_000
    private const val PASSWORD_SALT_BYTES = 16
    private const val PASSWORD_KEY_BITS = 256

    /** True when a password exists; the password itself is never returned. */
    fun hasAppPassword(context: Context): Boolean = getStoredPassword(context).isNotBlank()

    /** Backward-compatible getter: returns the legacy plaintext only for old installations. */
    @Deprecated("Use hasAppPassword/verifyAppPassword")
    fun getAppPassword(context: Context): String = getStoredPassword(context)

    fun verifyAppPassword(context: Context, candidate: String): Boolean {
        val stored = getStoredPassword(context)
        if (stored.isBlank()) return true
        if (stored.startsWith(PASSWORD_PREFIX)) return verifyPasswordHash(candidate, stored)
        // One-time migration from the old plaintext format.
        if (constantTimeEquals(candidate.toByteArray(Charsets.UTF_8), stored.toByteArray(Charsets.UTF_8))) {
            setAppPassword(context, candidate)
            return true
        }
        return false
    }

    fun setAppPassword(context: Context, newPassword: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (newPassword.isBlank()) {
            prefs.edit().remove(KEY_APP_PASSWORD).apply()
            return
        }
        prefs.edit().putString(KEY_APP_PASSWORD, hashPassword(newPassword)).apply()
    }

    private fun getStoredPassword(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_PASSWORD, "") ?: ""
    }

    private fun hashPassword(password: String): String {
        val salt = ByteArray(PASSWORD_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return PASSWORD_PREFIX + PASSWORD_ITERATIONS + "$" +
            Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun verifyPasswordHash(password: String, encoded: String): Boolean {
        return try {
            val parts = encoded.split('$')
            if (parts.size != 4 || parts[0] != "v2") return false
            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], Base64.NO_WRAP)
            val expected = Base64.decode(parts[3], Base64.NO_WRAP)
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, expected.size * 8)
            val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            constantTimeEquals(actual, expected)
        } catch (_: Exception) { false }
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    private const val KEY_PROCESSED_CONTENT_FILES = "processed_content_files"

    /** کلیدهای فایل‌های JSON محتوایی که قبلاً در دیتابیس ادغام شده‌اند (هر کلید: "اسم‌فایل:هش‌محتوا") */
    fun getProcessedContentFiles(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PROCESSED_CONTENT_FILES, emptySet()) ?: emptySet()
    }

    /** کل فهرست را با فهرست تازه جایگزین می‌کند (نه اضافه‌کردن) */
    fun setProcessedContentFiles(context: Context, keys: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_PROCESSED_CONTENT_FILES, keys).apply()
    }

    private const val KEY_LINE_SPACING = "line_spacing"

    /** ضریب فاصله خطوط متن (پیش‌فرض ۱.۴)؛ برای مثال ۱.۱ فشرده، ۱.۸ بازتر */
    fun getLineSpacing(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_LINE_SPACING, 1.4f)
    }

    fun setLineSpacing(context: Context, value: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_LINE_SPACING, value).apply()
    }

    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"

    /** آیا صفحه گوشی حین استفاده از برنامه خاموش/قفل نشود (پیش‌فرض: فعال) */
    fun getKeepScreenOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
    }

    fun setKeepScreenOn(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
    }

    private const val KEY_AUTO_DARK_MODE = "auto_dark_mode"

    /** تاریک/روشن خودکار بر اساس ساعت گوشی (پیش‌فرض: غیرفعال، یعنی دستی) */
    fun getAutoDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_DARK_MODE, false)
    }

    fun setAutoDarkMode(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_DARK_MODE, value).apply()
    }

    private fun localDayKey(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.YEAR) * 10000 +
            (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
            cal.get(java.util.Calendar.DAY_OF_MONTH)
    }

    private fun previousLocalDayKey(): Int {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return localDayKeyFrom(cal)
    }

    private fun localDayKeyFrom(cal: java.util.Calendar): Int =
        cal.get(java.util.Calendar.YEAR) * 10000 +
            (cal.get(java.util.Calendar.MONTH) + 1) * 100 +
            cal.get(java.util.Calendar.DAY_OF_MONTH)

    /** بین ساعت ۱۸ شب تا ۶ صبح، «شب» در نظر گرفته می‌شود */
    fun isNightTimeNow(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour >= 18 || hour < 6
    }
}
