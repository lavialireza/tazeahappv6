package com.example.bookapp.data

import android.content.Context
import com.example.bookapp.BuildConfig
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** بررسی دستی بروزرسانی و دریافت APK داخل خود برنامه؛ استفاده عادی برنامه آفلاین باقی می‌ماند. */
object UpdateHelper {
    private const val MANIFEST_ASSET = "update.json"
    private const val MAX_APK_BYTES = 300L * 1024L * 1024L

    data class UpdateInfo(
        val buildNumber: Int,
        val tagName: String,
        val downloadUrl: String,
        val isReleaseApk: Boolean,
        val versionName: String = tagName,
        val minSupportedVersion: Int = 0,
        val forceUpdate: Boolean = false,
        val releaseDate: String = "",
        val releaseNotes: List<String> = emptyList(),
        val sha256: String = ""
    )

    data class InstalledVersion(val buildNumber: Int, val versionName: String)

    /** نسخه واقعی نصب‌شده را از PackageManager می‌خواند؛ BuildConfig ممکن است
     * بعد از نصب یک APK جدید تا قبل از راه‌اندازی مجدد پردازش، مقدار قبلی باشد. */
    fun getInstalledVersion(context: Context): InstalledVersion {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else info.versionCode
        return InstalledVersion(code, info.versionName ?: "${code}")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun validateSha256(file: File, expected: String) {
        val normalized = expected.trim().lowercase()
        require(Regex("^[0-9a-f]{64}$").matches(normalized)) {
            "شناسه SHA-256 در سرور معتبر نیست؛ بروزرسانی متوقف شد."
        }
        val actual = sha256(file)
        require(actual == normalized) {
            "صحت فایل APK تأیید نشد (SHA-256 متفاوت است). فایل حذف و بروزرسانی متوقف شد."
        }
    }

    suspend fun checkForUpdate(currentVersionCode: Int): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            // برنامه در حالت عادی آفلاین است. فقط با زدن «بررسی بروزرسانی»
            // به سرور ثابت همان Flavor وصل می‌شود.
            val baseUrl = BuildConfig.UPDATE_SERVER_URL.trim().removeSuffix("/")
            require(baseUrl.startsWith("https://")) {
                "آدرس سرور بروزرسانی باید با https:// شروع شود. آدرس فعلی: $baseUrl"
            }
            val manifestUrl = "$baseUrl/update.json"
            val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Tazieh-Android-Updater")
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("بررسی بروزرسانی ناموفق بود: ${connection.responseCode}")
                }
                require(connection.url.protocol.equals("https", ignoreCase = true)) {
                    "سرور بروزرسانی به اتصال امن HTTPS منتقل نشد؛ بروزرسانی متوقف شد."
                }
                val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val expectedPackage = if (BuildConfig.PUBLIC_VIEWER) {
                    "com.example.bookapp.viewer"
                } else {
                    "com.example.bookapp"
                }
                val manifestPackage = json.optString("packageName", expectedPackage)
                if (manifestPackage != expectedPackage) {
                    throw IllegalStateException("این سرور برای نسخه دیگری از برنامه تنظیم شده است.")
                }

                val expectedAccess = if (BuildConfig.PUBLIC_VIEWER) "viewer" else "admin"
                val access = json.optString("access", expectedAccess).lowercase()
                if (access != expectedAccess) {
                    throw IllegalStateException("سرور بروزرسانی مربوط به ${if (BuildConfig.PUBLIC_VIEWER) "User" else "Admin"} نیست.")
                }

                val buildNumber = json.optInt("versionCode", 0)
                if (buildNumber <= 0) throw IllegalStateException("versionCode در update.json معتبر نیست.")
                if (buildNumber <= currentVersionCode) return@runCatching null

                val apkUrlRaw = json.optString("apkUrl").trim()
                if (apkUrlRaw.isBlank()) {
                    throw IllegalStateException("در update.json آدرس APK مشخص نشده است.")
                }
                val apkUrl = if (apkUrlRaw.startsWith("https://") || apkUrlRaw.startsWith("http://")) {
                    apkUrlRaw
                } else {
                    URL(URL("$baseUrl/"), apkUrlRaw.removePrefix("/")).toString()
                }
                require(apkUrl.startsWith("https://")) { "آدرس APK بروزرسانی باید امن (HTTPS) باشد." }
                val sha256 = json.optString("sha256").trim().lowercase()
                require(Regex("^[0-9a-f]{64}$").matches(sha256)) {
                    "در update.json مقدار SHA-256 معتبر برای APK وجود ندارد؛ بروزرسانی متوقف شد."
                }

                val notes = mutableListOf<String>()
                val notesArray = json.optJSONArray("releaseNotes")
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        notesArray.optString(i).takeIf { it.isNotBlank() }?.let(notes::add)
                    }
                }

                UpdateInfo(
                    buildNumber = buildNumber,
                    tagName = json.optString("tagName", "server-$buildNumber"),
                    downloadUrl = apkUrl,
                    isReleaseApk = true,
                    versionName = json.optString("versionName", "build$buildNumber"),
                    minSupportedVersion = json.optInt("minSupportedVersion", 0),
                    forceUpdate = json.optBoolean("forceUpdate", false),
                    releaseDate = json.optString("releaseDate", ""),
                    releaseNotes = notes,
                    sha256 = sha256
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    fun createUpdateManifest(context: Context, versionCode: Int, versionName: String, minSupportedVersion: Int, forceUpdate: Boolean, apkFile: String, releaseNotes: List<String>): File {
        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, MANIFEST_ASSET)
        val apk = File(apkFile)
        val apkSha256 = if (apk.isFile) sha256(apk) else ""
        val json = JSONObject().apply {
            put("appName", if (com.example.bookapp.BuildConfig.PUBLIC_VIEWER) "Tazieh Viewer" else "Tazieh Admin")
            put("versionCode", versionCode); put("versionName", versionName); put("minSupportedVersion", minSupportedVersion)
            put("forceUpdate", forceUpdate); put("apkFile", apkFile); put("sha256", apkSha256); put("releaseDate", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
            put("releaseNotes", JSONArray(releaseNotes))
        }
        file.writeText(json.toString(2), Charsets.UTF_8); return file
    }

    /**
     * APK را داخل cache خود برنامه دانلود می‌کند و پس از تکمیل، نصب سیستم را باز می‌کند.
     * این روش به مرورگر وابسته نیست و تا پایان دریافت صبر می‌کند.
     */
    suspend fun downloadAndInstall(
        context: Context,
        info: UpdateInfo,
        onProgress: (percent: Int) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // فایل کامل را در filesDir نگه می‌داریم تا اگر کاربر دوباره همان
            // بروزرسانی را درخواست کرد، APK دوباره از اینترنت دانلود نشود.
            val updateDir = File(context.filesDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "tazieh-update-${info.buildNumber}.apk")
            val partialFile = File(updateDir, "tazieh-update-${info.buildNumber}.apk.part")

            // فقط فایل نهایی را قابل نصب می‌دانیم؛ فایل .part ممکن است ناقص باشد.
            // اگر APK ذخیره‌شده دیگر از نسخه نصب‌شده جدیدتر نیست، آن را دوباره نصب نکن.
            if (apkFile.exists() && apkFile.length() > 0L) {
                val installed = getInstalledVersion(context)
                val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                val archiveCode = archiveInfo?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else it.versionCode
                }
                val samePackage = archiveInfo?.packageName == context.packageName
                if (archiveCode != null && samePackage && archiveCode > installed.buildNumber) {
                    validateSha256(apkFile, info.sha256)
                    // ملاک نصب، نسخه واقعی داخل خود APK است؛ برچسب Release گیت‌هاب
                    // فقط برای پیدا کردن بروزرسانی استفاده می‌شود و ممکن است با
                    // versionCode داخلی APK قدیمی/متفاوت باشد.
                    onProgress(100)
                    withContext(Dispatchers.Main) { installApk(context, apkFile) }
                    return@runCatching
                }
                if (archiveCode != null && samePackage && archiveCode <= installed.buildNumber) {
                    apkFile.delete()
                    throw IllegalStateException(
                        "فایل APK موجود (نسخه $archiveCode) از نسخه نصب‌شده (${installed.buildNumber}) جدیدتر نیست؛ بروزرسانی متوقف شد."
                    )
                }
                apkFile.delete()
            }
            if (partialFile.exists()) partialFile.delete()

            val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "Tazieh-Android-Updater")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("دریافت APK ناموفق بود: ${connection.responseCode}")
                }
                require(connection.url.protocol.equals("https", ignoreCase = true)) {
                    "دریافت APK به اتصال امن HTTPS منتقل نشد؛ بروزرسانی متوقف شد."
                }
                val total = connection.contentLengthLong
                if (total > MAX_APK_BYTES) throw IllegalStateException("حجم APK بروزرسانی بیش از حد مجاز است.")
                var received = 0L
                var lastPercent = -1
                connection.inputStream.use { input ->
                    partialFile.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            received += count
                            if (received > MAX_APK_BYTES) {
                                throw IllegalStateException("حجم APK بروزرسانی بیش از حد مجاز است.")
                            }
                            output.write(buffer, 0, count)
                            if (total > 0) {
                                val percent = ((received * 100L) / total).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (!partialFile.exists() || partialFile.length() == 0L) {
                throw IllegalStateException("فایل APK کامل دریافت نشد")
            }
            if (apkFile.exists()) apkFile.delete()
            if (!partialFile.renameTo(apkFile)) {
                throw IllegalStateException("ذخیره فایل APK نهایی ناموفق بود")
            }

            // قبل از نصب، نسخه واقعی داخل APK را بررسی می‌کنیم.
            // مهم: شماره tag گیت‌هاب فقط برای پیدا کردن Release است و الزاماً
            // نباید با versionCode داخلی APK مقایسه شود. ملاک نصب فقط این است
            // که APK متعلق به همین package و جدیدتر از نسخه نصب‌شده باشد.
            validateSha256(apkFile, info.sha256)

            val downloadedInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                ?: throw IllegalStateException("فایل دریافت‌شده یک APK معتبر نیست.")
            val downloadedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                downloadedInfo.longVersionCode.toInt()
            } else {
                downloadedInfo.versionCode
            }
            if (downloadedInfo.packageName != context.packageName) {
                apkFile.delete()
                throw IllegalStateException("این APK مربوط به همین برنامه نیست؛ بروزرسانی متوقف شد.")
            }
            val installedAfterDownload = getInstalledVersion(context)
            if (downloadedCode <= installedAfterDownload.buildNumber) {
                apkFile.delete()
                throw IllegalStateException(
                    "نسخه APK دریافت‌شده (${downloadedCode}) از نسخه نصب‌شده (${installedAfterDownload.buildNumber}) جدیدتر نیست؛ بروزرسانی متوقف شد."
                )
            }

            onProgress(100)
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            throw IllegalStateException("اجازه نصب برنامه از این منبع فعال نیست؛ پس از فعال‌سازی دوباره بروزرسانی را بزنید.")
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
