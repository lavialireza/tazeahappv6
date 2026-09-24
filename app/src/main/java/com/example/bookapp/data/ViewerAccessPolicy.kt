package com.example.bookapp.data

import com.example.bookapp.BuildConfig

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Locale
import java.security.MessageDigest

/** مدیریت سیاست دسترسی Viewer؛ بدون وابستگی به شبکه. */
object ViewerAccessPolicy {
    const val PROFILE_PUBLIC = "public"
    const val PROFILE_TRAINING = "training"
    const val PROFILE_COLLABORATOR = "collaborator"
    const val PROFILE_RESEARCHER = "researcher"
    const val PROFILE_DIRECTOR = "director"
    const val PROFILE_ACTOR = "actor"
    const val PROFILE_READER = "reader"
    const val PROFILE_CUSTOM = "custom"

    val permissionLabels = linkedMapOf(
        "read" to "مطالعه",
        "search" to "جستجو",
        "advancedSearch" to "جستجوی پیشرفته",
        "compare" to "مقایسه",
        "training" to "تمرین",
        "audio" to "صوت",
        "tts" to "تبدیل متن به گفتار",
        "notes" to "یادداشت",
        "bookmarks" to "علاقه‌مندی",
        "gallery" to "گالری تصاویر",
        "copy" to "کپی متن",
        "share" to "اشتراک‌گذاری",
        "pdf" to "PDF",
        "footnotes" to "پاورقی",
        "appIntro" to "معرفی برنامه",
        "myRole" to "نقش من",
        "dictionary" to "دیکشنری اصطلاحات تعزیه",
        "taziehCorrections" to "دیکشنری اصلاح و تصحیح متن تعزیه",
        "calendar" to "تقویم محرم",

        // مجوزهای فرزند؛ هر قابلیت اصلی والد مستقل است و فرزندان جزئیات واقعی آن را کنترل می‌کنند.
        "read.view" to "مطالعه: مشاهده متن",
        "read.navigate" to "مطالعه: جابه‌جایی بین بخش‌ها",
        "search.basic" to "جستجو: جستجوی معمولی",
        "advancedSearch.filters" to "جستجوی پیشرفته: فیلترها",
        "compare.view" to "مقایسه: مشاهده و اجرای مقایسه",
        "training.rehearse" to "تمرین: اجرای بازخوانی",
        "audio.play" to "صوت: پخش",
        "tts.play" to "تبدیل متن به گفتار: اجرا",
        "notes.view" to "یادداشت: مشاهده",
        "notes.add" to "یادداشت: افزودن",
        "notes.edit" to "یادداشت: ویرایش",
        "notes.delete" to "یادداشت: حذف",
        "bookmarks.view" to "علاقه‌مندی: مشاهده",
        "bookmarks.add" to "علاقه‌مندی: افزودن",
        "bookmarks.delete" to "علاقه‌مندی: حذف",
        "gallery.view" to "گالری: مشاهده",
        "gallery.add" to "گالری: افزودن تصویر",
        "gallery.edit" to "گالری: ویرایش توضیح تصویر",
        "gallery.delete" to "گالری: حذف تصویر",
        "gallery.largePreview" to "گالری: نمایش بزرگ",
        "copy.text" to "کپی: کپی متن",
        "share.content" to "اشتراک‌گذاری: اشتراک محتوا",
        "pdf.create" to "PDF: ایجاد خروجی",
        "pdf.save" to "PDF: ذخیره خروجی",
        "footnotes.view" to "پاورقی: مشاهده",
        "footnotes.add" to "پاورقی: افزودن",
        "footnotes.edit" to "پاورقی: ویرایش",
        "footnotes.delete" to "پاورقی: حذف",
        "footnoteSync.dictionary" to "پاورقی: همگام‌سازی با دیکشنری",
        "appIntro.view" to "معرفی برنامه: مشاهده",
        "myRole.view" to "نقش من: مشاهده نقش‌ها",
        "myRole.select" to "نقش من: انتخاب نقش",
        "myRole.remove" to "نقش من: حذف نقش انتخاب‌شده",
        "myRole.rehearse" to "نقش من: تمرین",
        "myRole.pdf" to "نقش من: خروجی PDF",
        "dictionary.view" to "دیکشنری: مشاهده اصطلاحات",
        "dictionary.add" to "دیکشنری اصطلاحات تعزیه: افزودن",
        "dictionary.edit" to "دیکشنری: ویرایش واژه",
        "dictionary.delete" to "دیکشنری: حذف واژه",
        "taziehCorrections.view" to "اصلاحات تعزیه: مشاهده",
        "taziehCorrections.add" to "اصلاحات تعزیه: افزودن اصلاح",
        "taziehCorrections.edit" to "اصلاحات تعزیه: ویرایش اصلاح",
        "taziehCorrections.delete" to "اصلاحات تعزیه: حذف اصلاح",
        "taziehCorrections.apply" to "اصلاحات تعزیه: اعمال اصلاح بر متن",
        "calendar.view" to "تقویم محرم: مشاهده",
        "calendar.suggestions" to "تقویم محرم: پیشنهاد تعزیه‌ها"
    )

    /** والد هر مجوز فرزند. اگر والد خاموش باشد، فرزند نیز مؤثرًا خاموش است. */
    val permissionParents = mapOf(
        "read.view" to "read", "read.navigate" to "read",
        "search.basic" to "search", "advancedSearch.filters" to "advancedSearch",
        "compare.view" to "compare", "training.rehearse" to "training",
        "audio.play" to "audio", "tts.play" to "tts",
        "notes.view" to "notes", "notes.add" to "notes", "notes.edit" to "notes", "notes.delete" to "notes",
        "bookmarks.view" to "bookmarks", "bookmarks.add" to "bookmarks", "bookmarks.delete" to "bookmarks",
        "gallery.view" to "gallery", "gallery.add" to "gallery", "gallery.edit" to "gallery", "gallery.delete" to "gallery", "gallery.largePreview" to "gallery",
        "copy.text" to "copy", "share.content" to "share",
        "pdf.create" to "pdf", "pdf.save" to "pdf",
        "footnotes.view" to "footnotes", "footnotes.add" to "footnotes",
        "footnotes.edit" to "footnotes", "footnotes.delete" to "footnotes",
        "footnoteSync.dictionary" to "footnotes",
        "appIntro.view" to "appIntro",
        "myRole.view" to "myRole", "myRole.select" to "myRole", "myRole.remove" to "myRole",
        "myRole.rehearse" to "myRole", "myRole.pdf" to "myRole",
        "dictionary.view" to "dictionary", "dictionary.add" to "dictionary", "dictionary.edit" to "dictionary", "dictionary.delete" to "dictionary",
        "taziehCorrections.view" to "taziehCorrections", "taziehCorrections.add" to "taziehCorrections",
        "taziehCorrections.edit" to "taziehCorrections", "taziehCorrections.delete" to "taziehCorrections", "taziehCorrections.apply" to "taziehCorrections",
        "calendar.view" to "calendar", "calendar.suggestions" to "calendar"
    )

    data class SpecialUser(
        val installationId: String,
        val profile: String,
        val expiresAt: Long?,
        val permissions: Map<String, Boolean>,
        val displayName: String = "",
        val details: String = "",
        val phone: String = "",
        val address: String = "",
        val position: String = "",
        val userType: String = "",
        val otherDetails: String = "",
        val enabled: Boolean = true,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val lastPolicySentAt: Long = 0L,
        val lastPolicySentFingerprint: String = "",
        val lastPolicySentVersion: Int = 0,
        val lastPolicyAppliedAt: Long = 0L,
        val lastPolicyAppliedVersion: Int = 0
    )

    private const val PREFS = "viewer_access_policy"
    private const val KEY_PUBLIC = "public_permissions"
    private const val KEY_SPECIAL = "special_users"
    private const val KEY_POLICY_VERSION = "policy_version"
    private const val KEY_POLICY_VERSIONS = "policy_versions"
    private const val KEY_IMPORTED_PUBLIC_VERSION = "imported_public_version"
    private const val KEY_IMPORTED_PUBLIC = "imported_public_permissions"
    private const val SPECIAL_USERS_FILE = "viewer_access_special_users.json"
    private const val SPECIAL_USER_HISTORY_FILE = "viewer_access_special_users_history.json"
    private const val MAX_HISTORY_PER_USER = 20

    fun defaultPermissions(): Map<String, Boolean> = permissionLabels.keys.associateWith { key ->
        key !in setOf("copy", "share", "pdf", "appIntro")
    }

    fun normalizedPermissions(input: Map<String, Boolean>): Map<String, Boolean> {
        val defaults = defaultPermissions()
        val result = defaults.toMutableMap()
        input.forEach { (key, value) -> if (key in permissionLabels) result[key] = value }
        // مهاجرت کلید قدیمی Stage53 به کلید جدید و دقیق‌تر.
        if (!input.containsKey("footnoteSync.dictionary") && input.containsKey("footnoteSync")) {
            result["footnoteSync.dictionary"] = input["footnoteSync"] == true
        }
        // نسخه‌های قدیمی فقط والدها را داشتند؛ فرزندان جدید همان وضعیت والد را به ارث می‌برند.
        permissionParents.forEach { (child, parent) ->
            if (!input.containsKey(child)) result[child] = result[parent] == true
        }
        return result
    }

    fun profileDefaults(profile: String): Map<String, Boolean> {
        val all = defaultPermissions().toMutableMap()
        fun set(vararg pairs: Pair<String, Boolean>) { pairs.forEach { all[it.first] = it.second } }
        return when (profile) {
            PROFILE_PUBLIC -> all
            PROFILE_TRAINING -> { set("training" to true, "training.rehearse" to true, "myRole" to true, "myRole.view" to true, "myRole.rehearse" to true); all }
            PROFILE_COLLABORATOR -> { set("copy" to true, "copy.text" to true, "share" to false, "share.content" to false, "pdf" to false, "pdf.create" to false, "pdf.save" to false); all }
            PROFILE_RESEARCHER -> { set("advancedSearch" to true, "advancedSearch.filters" to true, "compare" to true, "compare.view" to true, "footnotes" to true, "footnotes.view" to true, "footnotes.add" to true, "footnotes.edit" to true, "footnotes.delete" to true, "footnoteSync.dictionary" to true, "dictionary" to true, "dictionary.view" to true, "dictionary.add" to true, "dictionary.edit" to true, "dictionary.delete" to true, "taziehCorrections" to true, "taziehCorrections.view" to true, "taziehCorrections.add" to true, "taziehCorrections.edit" to true, "taziehCorrections.apply" to true); all }
            PROFILE_DIRECTOR -> { set("gallery" to true, "gallery.view" to true, "gallery.add" to true, "gallery.edit" to true, "gallery.delete" to true, "gallery.largePreview" to true, "myRole" to true, "myRole.view" to true, "myRole.select" to true, "myRole.rehearse" to true, "myRole.pdf" to true, "pdf" to true, "pdf.create" to true, "pdf.save" to true, "share" to true, "share.content" to true); all }
            PROFILE_ACTOR -> { set("myRole" to true, "myRole.view" to true, "myRole.select" to true, "myRole.remove" to true, "myRole.rehearse" to true, "audio" to true, "audio.play" to true, "tts" to true, "tts.play" to true, "notes" to true, "notes.view" to true, "notes.add" to true, "notes.edit" to true, "bookmarks" to true, "bookmarks.view" to true, "bookmarks.add" to true); all }
            PROFILE_READER -> { set("read" to true, "read.view" to true, "read.navigate" to true, "search" to true, "search.basic" to true, "bookmarks" to true, "bookmarks.view" to true, "bookmarks.add" to true); all }
            else -> all
        }
    }


    /**
     * دسترسی مؤثر Viewer.
     *
     * نکته امنیتی Stage77: flavor عمومی باید واقعاً برای محتوای اصلی برنامه
     * فقط خواندنی باشد؛ بنابراین حتی اگر یک فایل سیاست قدیمی/دستی مجوز
     * ویرایش فرهنگ واژه، اصلاحات، پاورقی یا گالری بدهد، این مجوزها در Viewer
     * عمومی دوباره مسدود می‌شوند. یادداشت و علاقه‌مندی شخصی همچنان قابل مدیریت
     * هستند و به محتوای اصلی برنامه دست نمی‌زنند.
     */
    fun getEffectivePermissions(context: Context): Map<String, Boolean> {
        val id = installationId(context)
        val special = getSpecialUsers(context).firstOrNull { it.installationId == id }
        val permissions = if (special != null) {
            // وجود رکورد خاص یعنی این دستگاه صراحتاً مدیریت شده است؛
            // کاربر غیرفعال یا منقضی نباید دوباره به مجوزهای عمومی برگردد.
            if (!special.enabled) return emptyMap()
            val expiry = special.expiresAt
            if (expiry != null && expiry > 0L && System.currentTimeMillis() > expiry) return emptyMap()
            normalizedPermissions(special.permissions)
        } else {
            normalizedPermissions(getPublicPermissions(context))
        }.toMutableMap()

        if (BuildConfig.PUBLIC_VIEWER) {
            // Viewer عمومی: هیچ قابلیت تغییردهنده محتوای اصلی مجاز نیست.
            val protectedWrites = setOf(
                "gallery.add", "gallery.edit", "gallery.delete",
                "footnotes.add", "footnotes.edit", "footnotes.delete",
                "footnoteSync.dictionary",
                "dictionary.add", "dictionary.edit", "dictionary.delete",
                "taziehCorrections.add", "taziehCorrections.edit",
                "taziehCorrections.delete", "taziehCorrections.apply"
            )
            protectedWrites.forEach { key -> permissions[key] = false }
            // والدها نیز باید خاموش شود؛ وگرنه والد/فرزند می‌تواند رفتار UI را
            // در نسخه‌های قدیمی‌تر به شکل ناهمسان فعال کند. مشاهده گالری/پاورقی/
            // دیکشنری/اصلاحات همچنان مجاز می‌ماند.
        }
        return permissions
    }

    fun hasPermission(context: Context, key: String): Boolean {
        val permissions = getEffectivePermissions(context)
        if (key !in permissionLabels) return false
        val parent = permissionParents[key]
        return permissions[key] == true && (parent == null || permissions[parent] == true)
    }

    internal fun hasPermissionForTest(permissions: Map<String, Boolean>, key: String): Boolean {
        if (key !in permissionLabels) return false
        val parent = permissionParents[key]
        return permissions[key] == true && (parent == null || permissions[parent] == true)
    }

    internal fun setImportedPublicPermissions(context: Context, permissions: Map<String, Boolean>, version: Int) {
        val obj = JSONObject(); permissionLabels.keys.forEach { obj.put(it, permissions[it] == true) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_IMPORTED_PUBLIC, obj.toString())
            .putInt(KEY_IMPORTED_PUBLIC_VERSION, version)
            .putString(KEY_PUBLIC, obj.toString())
            .apply()
    }

    internal fun setImportedSpecialPermissions(
        context: Context,
        permissions: Map<String, Boolean>,
        expiresAt: Long?,
        version: Int,
        profile: String = PROFILE_CUSTOM,
        enabled: Boolean = true
    ) {
        val id = installationId(context)
        // هنگام دریافت سیاست فقط «بخش دسترسی» تغییر می‌کند؛ اطلاعات مدیریتی
        // موجود روی Viewer (نام، تلفن، آدرس و...) نباید با سیاست جایگزین شود.
        val normalizedProfile = when (profile) {
            PROFILE_PUBLIC, PROFILE_TRAINING, PROFILE_COLLABORATOR, PROFILE_CUSTOM -> profile
            else -> PROFILE_CUSTOM
        }
        val existing = getSpecialUsers(context).firstOrNull { it.installationId == id }
        val imported = SpecialUser(
            installationId = id,
            profile = normalizedProfile,
            expiresAt = expiresAt,
            permissions = permissions,
            displayName = existing?.displayName ?: "",
            details = existing?.details ?: "",
            phone = existing?.phone ?: "",
            address = existing?.address ?: "",
            position = existing?.position ?: "",
            userType = existing?.userType ?: "",
            otherDetails = existing?.otherDetails ?: "",
            enabled = enabled,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastPolicySentAt = existing?.lastPolicySentAt ?: 0L,
            lastPolicySentFingerprint = existing?.lastPolicySentFingerprint ?: "",
            lastPolicySentVersion = existing?.lastPolicySentVersion ?: 0,
            lastPolicyAppliedAt = existing?.lastPolicyAppliedAt ?: 0L,
            lastPolicyAppliedVersion = existing?.lastPolicyAppliedVersion ?: 0
        )
        upsertSpecialUser(context, imported)
        check(getSpecialUsers(context).firstOrNull { it.installationId == id }?.profile == normalizedProfile) {
            "پروفایل کاربر خاص پس از اعمال سیاست قابل بازیابی نیست."
        }
    }

    fun getPublicPermissions(context: Context): Map<String, Boolean> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PUBLIC, null)
        if (raw.isNullOrBlank()) return defaultPermissions()
        return runCatching {
            val obj = JSONObject(raw)
            permissionLabels.keys.associateWith { key -> obj.optBoolean(key, defaultPermissions()[key] == true) }
        }.getOrElse { defaultPermissions() }
    }

    fun setPublicPermissions(context: Context, permissions: Map<String, Boolean>) {
        val obj = JSONObject()
        permissionLabels.keys.forEach { obj.put(it, permissions[it] == true) }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_POLICY_VERSION, 1) + 1
        val versions = runCatching { JSONObject(prefs.getString(KEY_POLICY_VERSIONS, "{}") ?: "{}") }.getOrElse { JSONObject() }
        versions.put("*", versions.optInt("*", 0) + 1)
        prefs.edit().putString(KEY_PUBLIC, obj.toString()).putString(KEY_POLICY_VERSIONS, versions.toString()).putInt(KEY_POLICY_VERSION, next).apply()
    }

    fun getPolicyVersion(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_POLICY_VERSION, 1)

    /** نسخه مستقل سیاست برای هر مقصد؛ از برخورد نسخه کاربران مختلف جلوگیری می‌کند. */
    fun getPolicyVersion(context: Context, targetInstallationId: String): Int {
        val target = targetInstallationId.trim().uppercase(java.util.Locale.US).ifBlank { "*" }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_POLICY_VERSIONS, null)
        return runCatching { JSONObject(raw ?: "{}").optInt(target, 1).coerceAtLeast(1) }.getOrDefault(1)
    }

    private fun bumpPolicyVersion(context: Context, targetInstallationId: String) {
        val target = targetInstallationId.trim().uppercase(java.util.Locale.US).ifBlank { "*" }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val obj = runCatching { JSONObject(prefs.getString(KEY_POLICY_VERSIONS, "{}") ?: "{}") }.getOrElse { JSONObject() }
        val next = obj.optInt(target, 0).coerceAtLeast(0) + 1
        obj.put(target, next)
        check(prefs.edit().putString(KEY_POLICY_VERSIONS, obj.toString()).putInt(KEY_POLICY_VERSION, maxOf(prefs.getInt(KEY_POLICY_VERSION, 1), next)).commit()) {
            "نسخه سیاست ذخیره نشد."
        }
    }

    fun getSpecialUsers(context: Context): List<SpecialUser> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefRaw = prefs.getString(KEY_SPECIAL, null)
        val file = File(context.filesDir, SPECIAL_USERS_FILE)
        // فایل داخلی به عنوان پشتیبان پایدار نگه داشته می‌شود. اگر SharedPreferences
        // خالی/خراب باشد، رکوردهای ذخیره‌شده از فایل بازیابی می‌شوند.
        val raw = when {
            !prefRaw.isNullOrBlank() -> prefRaw
            file.exists() -> runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
            else -> null
        } ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val id = o.optString("installationId").trim().uppercase(java.util.Locale.US)
                if (id.isBlank()) return@mapNotNull null
                val p = o.optJSONObject("permissions")
                val perms = normalizedPermissions(permissionLabels.keys.associateWith { p?.optBoolean(it, false) ?: false })
                SpecialUser(
                    installationId = id,
                    profile = o.optString("profile", PROFILE_CUSTOM),
                    expiresAt = if (o.isNull("expiresAt")) null else o.optLong("expiresAt"),
                    permissions = perms,
                    displayName = o.optString("displayName", ""),
                    details = o.optString("details", ""),
                    phone = o.optString("phone", ""),
                    address = o.optString("address", ""),
                    position = o.optString("position", ""),
                    userType = o.optString("userType", ""),
                    otherDetails = o.optString("otherDetails", ""),
                    enabled = o.optBoolean("enabled", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    lastPolicySentAt = o.optLong("lastPolicySentAt", 0L),
                    lastPolicySentFingerprint = o.optString("lastPolicySentFingerprint", ""),
                    lastPolicySentVersion = o.optInt("lastPolicySentVersion", 0),
                    lastPolicyAppliedAt = o.optLong("lastPolicyAppliedAt", 0L),
                    lastPolicyAppliedVersion = o.optInt("lastPolicyAppliedVersion", 0)
                )
            }
        }.getOrElse { emptyList() }
    }

    fun saveSpecialUsers(context: Context, users: List<SpecialUser>, bumpVersion: Boolean = true) {
        val arr = JSONArray()
        users.forEach { user ->
            val o = JSONObject()
                .put("installationId", user.installationId)
                .put("profile", user.profile)
                .put("displayName", user.displayName)
                .put("details", user.details)
                .put("phone", user.phone)
                .put("address", user.address)
                .put("position", user.position)
                .put("userType", user.userType)
                .put("otherDetails", user.otherDetails)
                .put("enabled", user.enabled)
                .put("createdAt", user.createdAt)
                .put("updatedAt", user.updatedAt)
            .put("lastPolicySentAt", user.lastPolicySentAt)
            .put("lastPolicySentFingerprint", user.lastPolicySentFingerprint)
            .put("lastPolicySentVersion", user.lastPolicySentVersion)
            .put("lastPolicyAppliedAt", user.lastPolicyAppliedAt)
            .put("lastPolicyAppliedVersion", user.lastPolicyAppliedVersion)
            if (user.expiresAt == null) o.put("expiresAt", JSONObject.NULL) else o.put("expiresAt", user.expiresAt)
            val p = JSONObject(); permissionLabels.keys.forEach { p.put(it, user.permissions[it] == true) }
            o.put("permissions", p); arr.put(o)
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_POLICY_VERSION, 1) + 1
        // commit() is intentional here: the Admin screen immediately reloads the list
        // and the policy may be exported to Viewer in the same UI action.
        val json = arr.toString()
        val editor = prefs.edit().putString(KEY_SPECIAL, json)
        if (bumpVersion) editor.putInt(KEY_POLICY_VERSION, next)
        val saved = editor.commit()
        check(saved) { "ذخیره کاربران خاص در حافظه برنامه انجام نشد." }
        // یک نسخه مستقل در حافظه داخلی برنامه هم نگه می‌داریم تا رکورد کاربر خاص
        // با بازشدن دوباره صفحه/فرآیند برنامه قابل بازیابی باشد.
        val file = File(context.filesDir, SPECIAL_USERS_FILE)
        val tmp = File(context.filesDir, "$SPECIAL_USERS_FILE.tmp")
        runCatching {
            tmp.writeText(json, Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.writeText(json, Charsets.UTF_8)
                tmp.delete()
            }
        }.getOrElse { throw IllegalStateException("فایل پایدار کاربران خاص ذخیره نشد: ${it.message ?: "خطای نامشخص"}", it) }
        check(file.exists() && file.readText(Charsets.UTF_8) == json) { "تأیید ذخیره کاربران خاص انجام نشد." }
    }

    /** آخرین نسخه‌های قبلی پرونده کاربر خاص؛ فقط در Admin نگهداری می‌شود. */
    data class SpecialUserHistory(val capturedAt: Long, val user: SpecialUser)

    private fun historyFile(context: Context) = File(context.filesDir, SPECIAL_USER_HISTORY_FILE)

    private fun historyObject(user: SpecialUser, capturedAt: Long): JSONObject = JSONObject()
        .put("capturedAt", capturedAt)
        .put("user", specialUserToJson(user))

    private fun specialUserToJson(user: SpecialUser): JSONObject {
        val o = JSONObject()
            .put("installationId", user.installationId)
            .put("profile", user.profile)
            .put("displayName", user.displayName)
            .put("details", user.details)
            .put("phone", user.phone)
            .put("address", user.address)
            .put("position", user.position)
            .put("userType", user.userType)
            .put("otherDetails", user.otherDetails)
            .put("enabled", user.enabled)
            .put("createdAt", user.createdAt)
            .put("updatedAt", user.updatedAt)
            .put("lastPolicySentAt", user.lastPolicySentAt)
            .put("lastPolicySentFingerprint", user.lastPolicySentFingerprint)
            .put("lastPolicySentVersion", user.lastPolicySentVersion)
        if (user.expiresAt == null) o.put("expiresAt", JSONObject.NULL) else o.put("expiresAt", user.expiresAt)
        val p = JSONObject(); permissionLabels.keys.forEach { p.put(it, user.permissions[it] == true) }; o.put("permissions", p)
        return o
    }

    private fun jsonToSpecialUser(o: JSONObject): SpecialUser {
        val pp = o.optJSONObject("permissions")
        return SpecialUser(
            installationId = o.optString("installationId").trim().uppercase(Locale.US),
            profile = o.optString("profile", PROFILE_CUSTOM),
            expiresAt = if (o.isNull("expiresAt")) null else o.optLong("expiresAt"),
            permissions = permissionLabels.keys.associateWith { k -> pp?.optBoolean(k, false) ?: false },
            displayName = o.optString("displayName"), details = o.optString("details"), phone = o.optString("phone"),
            address = o.optString("address"), position = o.optString("position"), userType = o.optString("userType"),
            otherDetails = o.optString("otherDetails"), enabled = o.optBoolean("enabled", true),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()), updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            lastPolicySentAt = o.optLong("lastPolicySentAt", 0L),
                    lastPolicySentFingerprint = o.optString("lastPolicySentFingerprint", ""),
                    lastPolicySentVersion = o.optInt("lastPolicySentVersion", 0),
                    lastPolicyAppliedAt = o.optLong("lastPolicyAppliedAt", 0L),
                    lastPolicyAppliedVersion = o.optInt("lastPolicyAppliedVersion", 0)
        )
    }

    private fun recordSpecialUserHistory(context: Context, user: SpecialUser) {
        val file = historyFile(context)
        val root = runCatching { if (file.exists()) JSONObject(file.readText(Charsets.UTF_8)) else JSONObject() }.getOrElse { JSONObject() }
        val key = user.installationId.trim().uppercase(Locale.US)
        val arr = runCatching { root.optJSONArray(key) ?: JSONArray() }.getOrElse { JSONArray() }
        val rebuilt = JSONArray()
        rebuilt.put(historyObject(user, System.currentTimeMillis()))
        for (i in 0 until minOf(arr.length(), MAX_HISTORY_PER_USER - 1)) rebuilt.put(arr.getJSONObject(i))
        root.put(key, rebuilt)
        file.writeText(root.toString(), Charsets.UTF_8)
    }

    fun getSpecialUserHistory(context: Context, installationId: String): List<SpecialUserHistory> {
        val key = installationId.trim().uppercase(Locale.US)
        val file = historyFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONObject(file.readText(Charsets.UTF_8)).optJSONArray(key) ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val h = arr.optJSONObject(i) ?: return@mapNotNull null
                val u = h.optJSONObject("user") ?: return@mapNotNull null
                SpecialUserHistory(h.optLong("capturedAt", 0L), jsonToSpecialUser(u))
            }
        }.getOrElse { emptyList() }
    }

    /** اثرانگشت پایدار تنظیمات مؤثر بر سیاست؛ تغییر مجوز/پروفایل/انقضا/فعال‌بودن را تشخیص می‌دهد. */
    fun policyFingerprint(user: SpecialUser): String {
        val canonical = buildString {
            append(user.profile.trim().lowercase(Locale.US)).append('|')
            append(user.enabled).append('|')
            append(user.expiresAt ?: -1L).append('|')
            permissionLabels.keys.sorted().forEach { key ->
                append(key).append('=').append(user.permissions[key] == true).append(';')
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun policyNeedsResend(user: SpecialUser): Boolean =
        user.lastPolicySentAt <= 0L || user.lastPolicySentFingerprint.isBlank() ||
            user.lastPolicySentFingerprint != policyFingerprint(user)

    fun upsertSpecialUser(context: Context, user: SpecialUser) {
        val normalizedId = user.installationId.trim().uppercase(java.util.Locale.US)
        require(normalizedId.isNotBlank()) { "شناسه نصب خالی است." }
        val existing = getSpecialUsers(context).firstOrNull { it.installationId.trim().equals(normalizedId, ignoreCase = true) }
        val now = System.currentTimeMillis()
        val normalized = user.copy(
            installationId = normalizedId,
            createdAt = existing?.createdAt ?: user.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        existing?.let { recordSpecialUserHistory(context, it) }
        val users = getSpecialUsers(context).toMutableList()
        val index = users.indexOfFirst { it.installationId.trim().equals(normalizedId, ignoreCase = true) }
        if (index >= 0) users[index] = normalized else users.add(normalized)
        saveSpecialUsers(context, users)
        bumpPolicyVersion(context, normalizedId)
        check(getSpecialUsers(context).any { it.installationId == normalizedId }) { "کاربر خاص پس از ذخیره قابل بازیابی نیست." }
    }

    /** زمان آخرین ارسال موفق سیاست برای کاربر خاص را ثبت می‌کند. */
    fun markPolicySent(context: Context, installationId: String, policyVersion: Int): SpecialUser? {
        val normalizedId = installationId.trim().uppercase(Locale.US)
        val users = getSpecialUsers(context).toMutableList()
        val index = users.indexOfFirst { it.installationId.trim().uppercase(Locale.US) == normalizedId }
        if (index < 0) return null
        val current = users[index]
        require(policyVersion > 0) { "نسخه سیاست نامعتبر است." }
        val updated = current.copy(
            lastPolicySentAt = System.currentTimeMillis(),
            lastPolicySentFingerprint = policyFingerprint(current),
            lastPolicySentVersion = policyVersion
        )
        users[index] = updated
        saveSpecialUsers(context, users, bumpVersion = false)
        check(getSpecialUsers(context).firstOrNull { it.installationId == normalizedId }?.lastPolicySentAt == updated.lastPolicySentAt) {
            "زمان آخرین ارسال سیاست ذخیره نشد."
        }
        return updated
    }

    /** ثبت تأیید دریافت/اعمال سیاست که از Viewer برگشته است؛ بدون افزایش نسخه سیاست. */
    fun markPolicyApplied(context: Context, installationId: String, version: Int, fingerprint: String): SpecialUser? {
        val normalizedId = installationId.trim().uppercase(Locale.US)
        val users = getSpecialUsers(context).toMutableList()
        val index = users.indexOfFirst { it.installationId.trim().uppercase(Locale.US) == normalizedId }
        if (index < 0) return null
        val current = users[index]
        require(current.lastPolicySentVersion == version) { "تأیید Viewer مربوط به آخرین سیاست ارسال‌شده نیست." }
        require(current.lastPolicySentFingerprint.isNotBlank() && current.lastPolicySentFingerprint == fingerprint) { "اثر انگشت سیاست اعمال‌شده با سیاست ارسال‌شده مطابقت ندارد." }
        val updated = current.copy(lastPolicyAppliedAt = System.currentTimeMillis(), lastPolicyAppliedVersion = version)
        users[index] = updated
        saveSpecialUsers(context, users, bumpVersion = false)
        return updated
    }

    fun removeSpecialUser(context: Context, installationId: String) {
        val normalizedId = installationId.trim().uppercase(java.util.Locale.US)
        val existed = getSpecialUsers(context).any { it.installationId.trim().uppercase(java.util.Locale.US) == normalizedId }
        saveSpecialUsers(context, getSpecialUsers(context).filterNot { it.installationId.trim().uppercase(java.util.Locale.US) == normalizedId })
        if (existed) bumpPolicyVersion(context, normalizedId)
        check(getSpecialUsers(context).none { it.installationId.trim().uppercase(java.util.Locale.US) == normalizedId }) {
            "حذف کاربر خاص انجام نشد."
        }
    }

    fun installationId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString("installation_id", null)?.let { return it }
        val bytes = ByteArray(6); SecureRandom().nextBytes(bytes)
        val id = "VWR-" + bytes.joinToString("") { "%02X".format(it) }.chunked(4).joinToString("-")
        prefs.edit().putString("installation_id", id).commit().also { ok -> check(ok) { "شناسه نصب ذخیره نشد." } }
        return id
    }

    /** خروجی کامل تنظیمات مدیریت دسترسی برای پشتیبان/انتقال Admin. */
    fun exportSettingsJson(context: Context): String = toJson(context)

    /** وارد کردن کامل تنظیمات مدیریت دسترسی Admin با تأیید ساختار و جلوگیری از شناسه‌های تکراری. */
    fun importSettingsJson(context: Context, text: String): Result<String> = runCatching {
        val root = JSONObject(text)
        require(root.optInt("schema", -1) == 1) { "نسخه فایل تنظیمات پشتیبانی نمی‌شود." }
        val pub = root.optJSONObject("public") ?: JSONObject()
        val publicMap = permissionLabels.keys.associateWith { key -> pub.optBoolean(key, false) }
        val arr = root.optJSONArray("specialUsers") ?: JSONArray()
        val users = mutableListOf<SpecialUser>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            users += jsonToSpecialUser(o)
        }
        require(users.map { it.installationId }.distinct().size == users.size) { "در فایل، شناسه نصب تکراری وجود دارد." }
        setPublicPermissions(context, publicMap)
        saveSpecialUsers(context, users, bumpVersion = false)
        val importedVersion = root.optInt("policyVersion", 0)
        if (importedVersion > getPolicyVersion(context)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_POLICY_VERSION, importedVersion).commit()
        }
        check(getSpecialUsers(context).size == users.size) { "بازیابی کاربران خاص کامل نشد." }
        "تنظیمات دسترسی با موفقیت وارد و جایگزین شد."
    }

    fun toJson(context: Context): String {
        val root = JSONObject().put("schema", 1).put("policyVersion", getPolicyVersion(context))
        val pub = JSONObject(); getPublicPermissions(context).forEach { (k,v) -> pub.put(k,v) }; root.put("public", pub)
        val arr = JSONArray(); getSpecialUsers(context).forEach { u ->
            arr.put(specialUserToJson(u))
        }; root.put("specialUsers", arr)
        return root.toString(2)
    }
}
