package com.example.bookapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.bookapp.data.Prefs
import com.example.bookapp.data.ViewerAccessTransfer
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.migratePrefsUserDataToRoom
import com.example.bookapp.ui.AppNavigation
import com.example.bookapp.ui.theme.colorSchemeFor
import com.example.bookapp.ui.theme.typographyFor

class MainActivity : ComponentActivity() {
    private fun handleViewerAccessIntent(incoming: android.content.Intent?) {
        if (!BuildConfig.PUBLIC_VIEWER) return
        if (incoming?.action != android.content.Intent.ACTION_SEND) return
        val uri = incoming.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM) ?: return
        runCatching { contentResolver.openInputStream(uri) ?: error("فایل سیاست دسترسی خوانده نشد.") }
            .fold(
                onSuccess = { input -> ViewerAccessTransfer.importPolicy(this, input) },
                onFailure = { Result.failure(it) }
            ).onSuccess { message ->
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
                // سیاست جدید در SharedPreferences ذخیره شده است، اما Compose ممکن است
                // مجوزهای قبلی را در state نگه داشته باشد. Intent را پاک می‌کنیم تا
                // در recreate دوباره همان فایل وارد نشود، سپس Activity را بازسازی می‌کنیم
                // تا منوی Viewer و همه قابلیت‌ها فوراً بر اساس مجوزهای جدید refresh شوند.
                setIntent(android.content.Intent())
                recreate()
            }
            .onFailure { android.widget.Toast.makeText(this, "اعمال سیاست ناموفق بود: ${it.message ?: "فایل نامعتبر است."}", android.widget.Toast.LENGTH_LONG).show() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewerAccessIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleViewerAccessIntent(intent)
        if (BuildConfig.PUBLIC_VIEWER) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // ثبت خودکار کرش‌ها: در صورت کرش برنامه، جزئیات خطا در یک فایل داخل
        // حافظه‌ی اپ ذخیره می‌شود تا بعداً از تنظیمات قابل مشاهده/ارسال باشد
        // (چون به سرویس آنالیتیکس بیرونی وصل نیستیم، این ساده‌ترین راه محلی است).
        val defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val logFile = java.io.File(filesDir, "last_crash.txt")
                logFile.writeText(
                    "زمان: ${java.util.Date()}\n\n" + android.util.Log.getStackTraceString(throwable)
                )
            } catch (e: Exception) {
                // اگر نوشتن لاگ هم شکست خورد، کاری نمی‌شود کرد
            }
            defaultCrashHandler?.uncaughtException(thread, throwable)
        }

        // اگر اپ از طریق میان‌بر فشار طولانی روی آیکون باز شده، مقصد را می‌خوانیم
        val shortcutTarget = intent?.getStringExtra("shortcut_target")

        // اگر اپ از طریق یک لینک taziehapp://section/{id} باز شده (اشتراک‌گذاری مستقیم یک بخش)
        val deepLinkSectionId = intent?.data?.let { uri ->
            if (uri.scheme == "taziehapp" && uri.host == "section") uri.lastPathSegment?.toLongOrNull() else null
        }

        // مهاجرت تدریجی داده‌های قدیمی SharedPreferences به Room. این کار فقط داده‌های
        // کاربر را کپی می‌کند و تا زمان تکمیل مهاجرت UI، سازگاری نسخه قدیمی را حفظ می‌کند.
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { migratePrefsUserDataToRoom(this@MainActivity, AppDatabase.getInstance(this@MainActivity)) }
        }

        if (BuildConfig.PUBLIC_VIEWER) {
            val noticePrefs = getSharedPreferences("viewer_access_policy", MODE_PRIVATE)
            if (noticePrefs.getBoolean("pending_policy_change_notice", false)) {
                val version = noticePrefs.getInt("pending_policy_change_version", 0)
                android.widget.Toast.makeText(this, "تغییر سیاست دسترسی دریافت شد${if (version > 0) " — نسخه $version" else ""}. برای اعمال، فایل سیاست را از Admin دریافت کنید.", android.widget.Toast.LENGTH_LONG).show()
                noticePrefs.edit().remove("pending_policy_change_notice").remove("pending_policy_change_target").remove("pending_policy_change_version").apply()
            }
        }

        setContent {
            var autoDarkMode by remember { mutableStateOf(Prefs.getAutoDarkMode(this)) }
            var darkMode by remember {
                mutableStateOf(if (autoDarkMode) Prefs.isNightTimeNow() else Prefs.isDarkMode(this))
            }
            var fontScale by remember { mutableFloatStateOf(Prefs.getFontScale(this)) }
            var themeChoice by remember { mutableStateOf(Prefs.getThemeChoice(this)) }
            var fontChoice by remember { mutableStateOf(Prefs.getFontChoice(this)) }
            var keepScreenOn by remember { mutableStateOf(Prefs.getKeepScreenOn(this)) }

            // با تغییر تنظیم، بلافاصله روی پنجره اعمال می‌شود (هم می‌شود روشنش کرد هم خاموش)
            androidx.compose.runtime.LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // در اندروید ۱۳ به بعد، نمایش اعلان نیاز به اجازه‌ی صریح کاربر دارد
            val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { /* نتیجه را نادیده می‌گیریم؛ اگر رد شود فقط اعلان نشان داده نمی‌شود */ }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val colorScheme = colorSchemeFor(themeChoice, darkMode)
            val typography = typographyFor(fontChoice, fontScale)
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                            darkMode = darkMode,
                            onDarkModeChange = {
                                darkMode = it
                                Prefs.setDarkMode(this, it)
                            },
                            autoDarkMode = autoDarkMode,
                            onAutoDarkModeChange = {
                                autoDarkMode = it
                                Prefs.setAutoDarkMode(this, it)
                                if (it) darkMode = Prefs.isNightTimeNow()
                            },
                            fontScale = fontScale,
                            onFontScaleChange = {
                                fontScale = it
                                Prefs.setFontScale(this, it)
                            },
                            themeChoice = themeChoice,
                            onThemeChoiceChange = {
                                themeChoice = it
                                Prefs.setThemeChoice(this, it)
                            },
                            fontChoice = fontChoice,
                            onFontChoiceChange = {
                                fontChoice = it
                                Prefs.setFontChoice(this, it)
                            },
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnChange = {
                                keepScreenOn = it
                                Prefs.setKeepScreenOn(this, it)
                            },
                            shortcutTarget = shortcutTarget,
                            deepLinkSectionId = deepLinkSectionId
                        )
                }
            }
        }
    }
}
