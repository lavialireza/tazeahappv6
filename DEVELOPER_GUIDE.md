# راهنمای کامل توسعه‌دهنده — اپلیکیشن تعزیه (taziehapp)

این فایل طوری نوشته شده که بدون نیاز به کمک گرفتن از جای دیگه، بتونید با مراجعه بهش:
- بفهمید هر فایل چیکار می‌کنه،
- یک صفحه/قابلیت جدید اضافه کنید،
- یک قابلیت موجود رو حذف/غیرفعال کنید،
- خروجی (APK یا AAB) بگیرید،
- و محتوا یا خود برنامه رو آپدیت کنید.

> **آخرین بروزرسانی این راهنما:** بعد از اضافه‌شدن گفتگو، پاورقی، گالری تصاویر،
> صدای واقعی، پشتیبان‌گیری کامل، نوتیفیکیشن، حالت شب خودکار، و خروجی AAB.

فهرست مطالب:
1. [زبان و ابزارهای برنامه](#1-زبان-و-ابزارهای-برنامه)
2. [نقشه کلی معماری](#2-نقشه-کلی-معماری)
3. [توضیح تک‌تک فایل‌ها](#3-توضیح-تک‌تک-فایل‌ها)
4. [چطور یک صفحه جدید اضافه کنم؟](#4-چطور-یک-صفحه-جدید-اضافه-کنم)
5. [چطور یک قابلیت موجود را حذف/غیرفعال کنم؟](#5-چطور-یک-قابلیت-موجود-را-حذفغیرفعال-کنم)
6. [توضیح کدها بر اساس موضوع (چرخه‌های کاری)](#6-توضیح-کدها-بر-اساس-موضوع)
7. [چطور خروجی (APK یا AAB) بگیرم؟](#7-چطور-خروجی-apk-یا-aab-بگیرم)
8. [آپدیت برنامه، محتوا، و دیتابیس](#8-آپدیت-برنامه-محتوا-و-دیتابیس)
9. [اشکالات رایج و راه‌حل سریع](#9-اشکالات-رایج-و-راه‌حل-سریع)

---

## ۱. زبان و ابزارهای برنامه

| بخش | زبان/ابزار |
|---|---|
| اپلیکیشن اندروید | **Kotlin** + **Jetpack Compose** (UI اعلانی) |
| دیتابیس | **Room** (روی SQLite)، با Migration واقعی از نسخه ۵ به بعد |
| محتوای تعزیه | چند فایل **JSON** مستقل در `assets/content/` |
| تبدیل Word→JSON | **Python** (`scripts/docx_to_json.py`) |
| ساخت خروجی | **Gradle** (`build.gradle.kts`، نحو Kotlin) |
| تست Kotlin | JUnit + Robolectric (`app/src/test/...`) |
| تست Python | `pytest` (`scripts/tests/`) |
| CI | GitHub Actions (`.github/workflows/build.yml`) — تست، APK دیباگ، و AAB ریلیز |
| تصاویر (نمایش) | Coil (`io.coil-kt:coil-compose`) |
| اعلان‌ها | `androidx.core:core-ktx` (`NotificationCompat`) |
| ذخیره‌سازی خارجی (پشتیبان‌گیری) | Storage Access Framework (`ACTION_CREATE_DOCUMENT`/`ACTION_OPEN_DOCUMENT`) |

---

## ۲. نقشه کلی معماری

```
MainActivity.kt
   (تنظیمات سطح اپ: تم، فونت، قفل صفحه، حالت شب خودکار، اجازه نوتیفیکیشن،
    ثبت‌کننده کرش، خواندن Deep Link/میان‌بر)
        │
        ▼
AppNavigation.kt
   (مسیریاب مرکزی + همه‌ی واکشی‌های دیتابیس + انیمیشن گذر بین صفحات)
        │
        ├── ui/screens/   → فقط UI (بدون منطق دیتابیس مستقیم)
        └── data/         → دیتابیس Room، DAOها، Entityها، Helperها
```

قانون ثابت پروژه: **صفحات (`ui/screens`) هیچ‌وقت مستقیم با دیتابیس یا Prefs کار
نمی‌کنند.** واکشی/ذخیره‌ی داده همیشه در `AppNavigation.kt` (داخل `LaunchedEffect`
یا `scope.launch`) انجام می‌شود و به‌صورت پارامتر/کال‌بک به صفحه پاس داده می‌شود.

سلسله‌مراتب محتوا (بدون تغییر از اول پروژه):

```
Field (زمینه) → Tazieh (تعزیه) → Role (نقش) → Section (بخش، شامل متن شعر)
```

روی این ساختار پایه، حالا این‌ها هم اضافه شده‌اند (هرکدام جدولی جدا،
مستقل، با ارجاع/لینک به Section یا Tazieh، نه کپی محتوا):
- **Footnote** → به یک Section
- **Dialogue + DialogueTurn** → به یک Tazieh + چند Section
- **TaziehImage** → به یک Tazieh

---

## ۳. توضیح تک‌تک فایل‌ها

### فایل اصلی
- **`MainActivity.kt`** — تنها Activity برنامه. اینجا: خواندن تنظیمات اولیه
  (تم/فونت/قفل صفحه/حالت‌شب‌خودکار) از `Prefs`، تنظیم `FLAG_KEEP_SCREEN_ON`
  به‌صورت پویا، ثبت `Thread.setDefaultUncaughtExceptionHandler` برای گزارش کرش،
  درخواست اجازه‌ی `POST_NOTIFICATIONS`، و خواندن Deep Link
  (`taziehapp://section/{id}`) یا میان‌بر آیکون، و در نهایت صدازدن
  `AppNavigation(...)`.

### `ui/AppNavigation.kt` — قلب برنامه
همه‌ی route ها، همه‌ی واکشی دیتابیس، و انیمیشن گذر (`navAnimDuration`) اینجاست.
هر `composable("route") { ... }` یک مقصد است.

### `ui/screens/*.kt` — صفحات
| فایل | صفحه |
|---|---|
| `SplashScreen.kt` | اسپلش اولیه |
| `OnboardingScreen.kt` | آموزش اولین اجرا |
| `LoginScreen.kt` | ورود (رمز اختیاری) |
| `MainMenuScreen.kt` | منوی اصلی (شامل دسترسی به گالری عمومی و Changelog) |
| `ListScreen.kt` (`GenericListScreen`) | فهرست عمومی قابل‌استفاده مجدد |
| `TextScreen.kt` | نمایش یک بخش (+ `TextPagerScreen` برای ورق‌زدن)؛ شامل پخش TTS/صدای واقعی، پاورقی، بخش‌های مرتبط، بخش قبل/بعد، اشتراک‌گذاری/Deep Link، گزارش اشکال محتوا |
| `SearchScreen.kt` | جستجو (متن + پاورقی + گفتگو، با فیلتر زمینه/تعزیه) |
| `BookmarksScreen.kt` / `NotesScreen.kt` | نشان‌شده‌ها / یادداشت‌ها |
| `CompareScreen.kt` | مقایسه دو نقش (پس‌زمینه نقش دوم متفاوت) |
| `MyRoleScreen.kt` / `RehearsalScreen.kt` | نقش من / حالت تمرین |
| `TaziehIndexScreen.kt` | فهرست تعزیه (مرتب‌سازی بر اساس عدد یا ترتیب متن، قابل ویرایش/جابه‌جایی) |
| `DialoguesScreen.kt` / `DialogueBuilderScreen.kt` / `DialogueReaderScreen.kt` | لیست گفتگوها / ساخت گفتگو از بخش‌های موجود / خوانش با رنگ متفاوت هر نقش + خروجی PDF |
| `TaziehGalleryScreen.kt` | گالری تصاویر یک تعزیه (افزودن/حذف) |
| `AllImagesGalleryScreen.kt` | گالری تجمیعی همه‌ی تعزیه‌ها در منوی اصلی (فقط نمایش) |
| `ChangelogScreen.kt` | «چه چیزی جدیده» |
| `InfoScreens.kt` | `AboutScreen` (آمار + نمودار ۱۴روزه + مشخصات طراح) / `SettingsScreen` (تم، فونت، فاصله خطوط، قفل صفحه، حالت شب خودکار، رمز عبور، بروزرسانی محتوا، پشتیبان‌گیری، گزارش کرش) / `VersionScreen` |

### `data/` — دیتابیس و منطق
- **`Entities.kt`** — همه‌ی جدول‌ها: `FieldEntity`, `TaziehEntity`, `RoleEntity`
  (با `orderIndex`), `SectionEntity` (با `audioUrl`), `FootnoteEntity`,
  `DialogueEntity`, `DialogueTurnEntity`, `TaziehImageEntity`.
- **`NoteEntity.kt`** — یادداشت‌ها (مستقل، به بخش خاصی وصل نیست).
- **`Dao.kt`** — تمام DAO ها (`FieldDao` تا `TaziehImageDao`).
- **`SearchDao.kt`** — جستجو با `LIKE` (نه FTS — دلیلش را در بخش ۹ بخوانید)
  روی بخش‌ها + پاورقی‌ها (با `UNION`)، به‌علاوه `searchDialogues` جدا.
- **`AppDatabase.kt`** — تعریف دیتابیس (نسخه فعلی را در `version = ` همین فایل
  ببینید)، `syncLocalContentFiles` (بارگذاری تدریجی از assets)،
  `syncRemoteContent` (بروزرسانی اینترنتی، روی `Dispatchers.IO`)،
  `mergeContentFromJson` (منطق upsert اصلی، `internal` — برای اینکه از فایل
  تست هم صدا زده شود).
- **`Prefs.kt`** — تنظیمات ساده: بوکمارک، رمز، نقش‌های من، فایل‌های محتوای
  پردازش‌شده، تم/فونت/فاصله‌خطوط، قفل‌صفحه، حالت‌شب‌خودکار، آمار مطالعه
  (`markSectionRead` + `getActiveDaysLast` برای نمودار).
- **`PdfExportHelper.kt`** — `exportRoleToPdf`، `exportTaziehToPdf`، و
  `exportDialogueToPdf` (هرسه از یک تابع داخلی مشترک `exportPdfInternal`
  استفاده می‌کنند).
- **`BackupHelper.kt`** — `buildBackupJson`/`writeBackupToUri`/
  `restoreBackupFromUri`: پشتیبان‌گیری کامل (یادداشت، بوکمارک، پاورقی،
  نقش‌من، گفتگو) به‌صورت JSON، از طریق SAF (هم حافظه گوشی هم ابری).
- **`ImageStorageHelper.kt`** — کپی عکس/صدا از URI انتخابی کاربر به حافظه‌ی
  اختصاصی اپ (`copyImageToAppStorage`, `copyAudioToAppStorage`) + حذف.
- **`AudioPlayerHelper.kt`** — پخش صدای واقعی (فایل محلی یا URL)، با
  fallback به TTS (`SpeechHelper.kt`) اگر `audioUrl` نال باشد.
- **`NotificationHelper.kt`** — کانال + نمایش نوتیفیکیشن «محتوای جدید»
  (فقط محلی، بدون سرور push).

### `scripts/`
- **`docx_to_json.py`** — تبدیل Word→JSON با افزودن خودکار به `content/`
  (شماره‌گذاری خودکار فایل).
- **`tests/test_docx_to_json.py`** — تست پایتون.

### `.github/workflows/build.yml`
دو Job: `test` (Kotlin+Python)، بعد `build` که فقط اگر تست‌ها پاس شوند اجرا
می‌شود و هم **APK دیباگ** هم **AAB ریلیز** می‌سازد (بخش ۷).

### فایل‌های مستندات ریشه پروژه
- **`README.md`** — نحوه‌ی افزودن محتوای تازه.
- **`PRIVACY_POLICY.md`** — پیش‌نویس سیاست حریم خصوصی (باید خودتان تکمیل کنید).
- **`PLAY_STORE_DATA_SAFETY.md`** — جواب‌های آماده برای فرم Data Safety.
- **`local.properties.example`** — الگوی کلید امضای Release.

---

## ۴. چطور یک صفحه جدید اضافه کنم؟

**قدم ۱ — فایل Composable صفحه** در `ui/screens/`، فقط UI + پارامتر/کال‌بک،
بدون دسترسی مستقیم دیتابیس:

```kotlin
@Composable
fun MyNewScreen(someData: String, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("عنوان") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(someData)
        }
    }
}
```

**قدم ۲ — route در `AppNavigation.kt`:**
```kotlin
private const val ROUTE_MY_NEW = "my_new"
...
composable(ROUTE_MY_NEW) {
    var data by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { data = db.someDao().getSomething() }
    MyNewScreen(someData = data, onBack = { navController.popBackStack() })
}
```

**قدم ۳ — راه ورود:** یک `onOpenMyNew: () -> Unit` به صفحه‌ای که دکمه دارد
اضافه کنید و در `AppNavigation.kt` وصلش کنید به `navController.navigate(ROUTE_MY_NEW)`.

اگر صفحه آرگومان می‌خواهد: `"my_new/{someId}"` و در `composable` با
`backStackEntry.arguments?.getString("someId")`.

---

## ۵. چطور یک قابلیت موجود را حذف/غیرفعال کنم؟

**سطح ۱ (سریع، کم‌ریسک):** فقط دکمه/کارت ورودش را در UI کامنت/حذف کنید؛ کد
پشتش دست‌نخورده می‌ماند.

**سطح ۲ (حذف کامل صفحه):**
1. `composable(ROUTE_XXX)` را از `AppNavigation.kt` حذف کنید.
2. فایل `ui/screens/XxxScreen.kt` را حذف کنید.
3. هر `navController.navigate("xxx...")` را پیدا و حذف/جایگزین کنید.
4. اگر Entity/Dao مخصوصش بود و جای دیگر استفاده نمی‌شود، حذفش کنید — ولی
   یادتان باشد `version` دیتابیس را افزایش دهید و یک Migration واقعی
   (`DROP TABLE ...`) بنویسید (بخش ۸).

**سطح ۳ (پرچم فعال/غیرفعال، برای قابلیت‌های آزمایشی):**
```kotlin
private const val FEATURE_X_ENABLED = false
...
if (FEATURE_X_ENABLED) { MenuCard(...) }
```

---

## ۶. توضیح کدها بر اساس موضوع

### بارگذاری محتوا (و بروزرسانی واقعی، نه فقط افزودن)
`AppNavigation.kt` → `syncLocalContentFiles` → فایل‌های `assets/content/*.json`
که نسبت به دفعه‌ی قبل **تغییر کرده‌اند** (نه فقط فایل‌های تازه) را پیدا می‌کند.
تشخیص تغییر بر اساس `"اسم‌فایل:هش‌محتوا"` است، نه فقط اسم فایل — یعنی اگر
محتوای همان فایل را (حتی با یک حرف تغییر) ویرایش کنید و دوباره commit کنید،
دوباره پردازش می‌شود. سپس `mergeContentFromJson`:
- زمینه/تعزیه/نقش را بر اساس عنوان upsert می‌کند.
- مشخصات `author`/`authorEmail` هر تعزیه را (اگر در JSON آمده باشد) به‌روز می‌کند.
- برای هر بخش: اگر بخشی با **همان عنوان** زیر همان نقش موجود باشد، متنش
  **جایگزین** می‌شود (نه اضافه‌شدن نسخه‌ی تکراری)؛ وگرنه بخش تازه به انتها
  اضافه می‌شود. **محدودیت:** اگر عنوان بخش هم عوض شود، به‌عنوان بخش تازه
  شناسایی می‌شود و بخش قدیمی حذف نمی‌شود (فقط تغییر محتوای زیر همان عنوان،
  «ویرایش واقعی» حساب می‌شود).

اگر کاربر قبلاً از اپ استفاده کرده بود و فایلی جدید/تغییریافته پردازش شد،
`showNewContentNotification` صدا زده می‌شود.

### جستجو
`SearchScreen` → `db.searchDao().search/searchInField/searchInTazieh` (LIKE،
شامل پاورقی‌ها) + `searchDialogues` جدا برای اسم گفتگوها. نتیجه کلیک‌شده به
`text/{id}` یا `dialogue_reader/{id}` می‌رود.

### پخش صدا
`TextScreen` اگر `section.audioUrl` داشته باشد از `AudioPlayerHelper`
(فایل محلی کپی‌شده یا URL) استفاده می‌کند، وگرنه از `SpeechHelper` (TTS).
افزودن/حذف صدا از منوی ⋮ همان صفحه (`copyAudioToAppStorage` →
`sectionDao().updateAudioUrl`).

### گالری تصاویر
هر تعزیه گالری مخصوص خودش دارد (افزودن/حذف، `TaziehImageDao`). منوی اصلی
یک گالری **تجمیعی و فقط‌نمایشی** از همه‌ی تعزیه‌ها دارد
(`AllImagesGalleryScreen`، بدون دکمه افزودن/حذف).

### گفتگو
`DialogueBuilderScreen` بخش‌های یک تعزیه (از همه نقش‌ها) را لیست می‌کند؛
کاربر به ترتیب لمس می‌کند → `DialogueTurnEntity` هایی با `orderIndex` پشت‌سرهم
ساخته می‌شود (فقط لینک به `sectionId`، بدون کپی متن). `DialogueReaderScreen`
به هر نقش، بر اساس ترتیب ظاهرشدنش، یکی از دو رنگ ثابت می‌دهد تا نقش‌ها از هم
قابل تشخیص باشند.

### پاورقی
`FootnoteEntity` مستقیم به `sectionId` وصل است؛ در `TextScreen` زیر متن
نمایش/افزوده/ویرایش/حذف می‌شود.

### پشتیبان‌گیری
`SettingsScreen` → `ActivityResultContracts.CreateDocument`/`OpenDocument`
(پنجره‌ی انتخاب مسیر سیستم، شامل حافظه گوشی و هر سرویس ابری نصب‌شده) →
`writeBackupToUri`/`restoreBackupFromUri` در `BackupHelper.kt`.
بازیابی **افزودنی** است، نه جایگزینی (چیزی پاک نمی‌شود).

### نوتیفیکیشن و کرش
`NotificationHelper.kt` فقط محلی است (بدون Firebase/سرور). کرش‌ها در
`MainActivity.onCreate` با `Thread.setDefaultUncaughtExceptionHandler` در
`filesDir/last_crash.txt` ذخیره و از تنظیمات قابل ارسال (share) هستند.

### حالت شب خودکار
`Prefs.isNightTimeNow()` (ساعت ۱۸ تا ۶) در `MainActivity` هنگام باز شدن اپ
چک می‌شود اگر `autoDarkMode` فعال باشد؛ سوییچ دستی وقتی خودکار فعال است
غیرفعال (`enabled = !autoDarkMode`) می‌شود تا تناقض نداشته باشند.

### Deep Link
`taziehapp://section/{id}` در Manifest تعریف شده؛ `MainActivity` آن را
می‌خواند و به `AppNavigation` پاس می‌دهد؛ بعد از ورود مستقیم به همان بخش
می‌رود (`postLoginRoute()`).

### چیدمان صفحه‌های بزرگ (تبلت)
`TextScreen` عرض محتوای متن را با `Modifier.widthIn(max = 640.dp)` و
`CenterHorizontally` محدود می‌کند تا طول خط روی صفحه‌های بزرگ زیاد نشود.
اگر صفحه‌ی جدیدی اضافه کردید که محتوای متنی طولانی نمایش می‌دهد، همین الگو
را برایش هم به‌کار ببرید.

---

## ۷. چطور خروجی (APK یا AAB) بگیرم؟

### تفاوت APK و AAB
- **APK** — نصب مستقیم روی گوشی، برای تست.
- **AAB (Android App Bundle)** — فرمتی که **Google Play از سال‌ها پیش فقط
  همین را قبول می‌کند** برای انتشار. اگر قصد انتشار در Play Store دارید،
  AAB لازم دارید نه APK.

### روش ۱ — خودکار با GitHub Actions
با هر push به `main`، هم APK دیباگ هم AAB ریلیز ساخته می‌شود. در تب
**Actions**، روی آخرین اجرا، بخش **Artifacts** دو فایل می‌بینید:
`app-debug-...` (APK) و `app-release-bundle-...` (AAB).

> اگر Secret های امضا (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`,
> `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) را در تنظیمات مخزن گیت‌هاب
> (Settings → Secrets and variables → Actions) اضافه کنید، AAB امضاشده
> ساخته می‌شود. وگرنه بدون امضا ساخته می‌شود (قابل بررسی، ولی برای آپلود در
> Play Console باید امضا شود).

### روش ۲ — Android Studio
`Build → Build Bundle(s) / APK(s) → Build Bundle(s)` برای AAB، یا
`... → Build APK(s)` برای APK.

### روش ۳ — خط فرمان
```bash
./gradlew assembleDebug     # APK دیباگ
./gradlew bundleRelease     # AAB ریلیز
```

---

## ۸. آپدیت برنامه، محتوا، و دیتابیس

### الف) آپدیت محتوا (بدون نیاز به نسخه جدید اپ، تا حدی)
همان‌طور که در `README.md` توضیح داده شده: `python scripts/docx_to_json.py
file.docx` یک فایل تازه در `assets/content/` می‌سازد که فقط با نصب نسخه‌ی
جدید اپ اضافه می‌شود (چون داخل بسته‌ی APK/AAB پکیج می‌شود).

### ب) آپدیت محتوا از راه دور (واقعاً بدون نسخه جدید اپ)
`syncRemoteContent` در `AppDatabase.kt` — از دکمه‌ی «بروزرسانی محتوا» در
تنظیمات صدا زده می‌شود، محتوا را merge می‌کند (بدون پاک‌کردن قبلی).

### ج) آپدیت خود برنامه
شماره نسخه (`versionCode`/`versionName`) در `app/build.gradle.kts` **خودکار
از تاریخچه Git** ساخته می‌شود (تعداد کامیت + هش کوتاه) — نیازی به دستی
تغییردادنش نیست.

### د) تغییر ساختار دیتابیس (مهم‌ترین بخش این فصل)
1. تغییر را در `Entities.kt`/`Dao.kt` اعمال کنید.
2. عدد `version` در `@Database(...)` داخل `AppDatabase.kt` را **حتماً** یکی
   افزایش دهید.
3. **از نسخه ۵ به بعد، دیگر از `fallbackToDestructiveMigration` به‌تنهایی
   استفاده نمی‌کنیم** — یک Migration واقعی بنویسید تا داده‌ی کاربر
   (بوکمارک/یادداشت/پاورقی/نقش‌من/گفتگو) پاک نشود:
   ```kotlin
   private val MIGRATION_N_N1 = object : Migration(N, N + 1) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE ... ADD COLUMN ...")
           // یا CREATE TABLE برای جدول تازه
       }
   }
   ```
   و آن را به `.addMigrations(...)` در `getInstance` اضافه کنید.
   `fallbackToDestructiveMigration()` را نگه دارید فقط به‌عنوان شبکه‌ی
   ایمنی برای گذارهای خیلی قدیمی‌تر که مطمئن نیستید.
4. اگر ستون تازه‌ای اضافه کردید که باید مقدار پیش‌فرض داشته باشد (مثلاً
   `orderIndex: Int = 0`)، در دستور `ALTER TABLE` هم `DEFAULT` بگذارید.

---

## ۹. اشکالات رایج و راه‌حل سریع

| خطا/مشکل | دلیل رایج | راه‌حل |
|---|---|---|
| `Unresolved reference: X` در کامپایل | import فراموش‌شده | همان کلاس/تابع را در فایل مشابه پیدا کنید و importش را کپی کنید |
| یک تابع/کلاس در فایل دیگری از همان پکیج «Unresolved» است | تابع/extension function `private` است (private در Kotlin مخصوص همان فایل است، نه کل پکیج) | به `internal` تغییر بدهید (مثل `mergeContentFromJson`) |
| صفحه‌ای بخشی از محتوایش دیده نمی‌شود / از پایین قطع می‌شود | `Column` بدون `.verticalScroll(rememberScrollState())` است | این Modifier را اضافه کنید (این باگ چندبار در `SettingsScreen`/`AboutScreen` پیش آمده بود، مراقب صفحات جدید هم باشید) |
| کامپایل با `Unresolved reference: util` در `build.gradle.kts` | پلاگین اندروید یک extension به اسم `java` دارد که با `java.util.Properties()` تداخل می‌کند | بالای فایل `import java.util.Properties` بنویسید و مستقیم `Properties()` صدا بزنید |
| بعد از ادیت با ابزار خودکار/AI، «Redeclaration» یا رفتار عجیب یک کلاس | یک Entity/Dao به‌اشتباه چندبار در فایل تکرار شده (این اتفاق چندبار افتاده) | بعد از هر ادیت بزرگ، بررسی کنید: `grep -c "^data class NameEntity(" Entities.kt` باید ۱ باشد |
| جستجوی فارسی نتیجه‌ای پیدا نمی‌کند | اگر روزی خواستید دوباره از FTS4 استفاده کنید: tokenizer پیش‌فرض و حتی `unicode61` روی همه‌ی گوشی‌ها پشتیبانی نمی‌شود | پروژه فعلاً عمداً از `LIKE` استفاده می‌کند، نه FTS — قبل از تغییرش مطمئن شوید |
| بعد از آپدیت، محتوای جدید اضافه نشده | فایل JSON جدید در `assets/content/` نیست یا اسمش تکراری است | اسم فایل باید منحصربه‌فرد باشد |
| بعد از تغییر Entity، اپ کاربران قبلی کرش می‌کند یا داده خالی است | Migration واقعی ننوشته‌اید یا `version` را افزایش نداده‌اید | بخش ۸-د را ببینید |
| Build با خطای Gradle/Dependency شکست می‌خورد | نسخه یک کتابخانه ناسازگار است | پیام کامل خطا معمولاً اسم کتابخانه را می‌گوید |
