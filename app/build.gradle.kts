import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// اطلاعات امضای Release را از local.properties یا متغیرهای محیطی می‌خواند.
// این فایل هرگز نباید حاوی کلید واقعی باشد و local.properties هم در .gitignore است.
val localProps = Properties()
run {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.inputStream().use { stream -> localProps.load(stream) }
    }
}
val updateServerProps = Properties()
run {
    val f = rootProject.file("update-servers.properties")
    if (f.exists()) {
        f.inputStream().use { stream -> updateServerProps.load(stream) }
    }
}
fun signingProp(key: String): String? =
    (localProps.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

// آدرس سرور بروزرسانی برای هر نسخه جداست و بدون تغییر کد Kotlin قابل تغییر است.
// اولویت: -P هنگام Build، سپس local.properties، و در نهایت مقدار پیش‌فرض.
fun updateServerProp(key: String, defaultValue: String): String =
    (providers.gradleProperty(key).orNull
        ?: localProps.getProperty(key)
        ?: updateServerProps.getProperty(key)
        ?: defaultValue).trim().removeSuffix("/")

val adminUpdateServerUrl = updateServerProp(
    "UPDATE_SERVER_ADMIN_URL",
    "https://example.com/tazieh/admin"
)
val viewerUpdateServerUrl = updateServerProp(
    "UPDATE_SERVER_VIEWER_URL",
    "https://example.com/tazieh/user"
)

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.example.bookapp"
    flavorDimensions += "access"

    productFlavors {
        create("admin") {
            dimension = "access"
            applicationId = "com.example.bookapp"
            buildConfigField("Boolean", "PUBLIC_VIEWER", "false")
            buildConfigField("String", "UPDATE_SERVER_URL", "\"${adminUpdateServerUrl}\"")
            buildConfigField("String", "SYNC_SERVER_URL", "\"\"")
            manifestPlaceholders["appLabel"] = "تعزیه و شبیه‌خوانی — مدیر"
        }
        create("viewer") {
            dimension = "access"
            applicationId = "com.example.bookapp.viewer"
            buildConfigField("Boolean", "PUBLIC_VIEWER", "true")
            buildConfigField("String", "UPDATE_SERVER_URL", "\"${viewerUpdateServerUrl}\"")
            buildConfigField("String", "SYNC_SERVER_URL", "\"\"")
            manifestPlaceholders["appLabel"] = "تعزیه و شبیه‌خوانی"
        }
    }
    compileSdk = 34

    // The Viewer keeps its protected content in src/viewer/assets.
    // Explicitly attach that directory to the viewer flavor so encrypted .taz
    // files are packaged into the Viewer APK under assets/content/.
    sourceSets {
        // Keep the common source set completely free of content assets.
        // Content is intentionally isolated per flavor so Admin JSON can
        // never become an input to the public Viewer variant.
        getByName("main") {
            assets.setSrcDirs(emptyList<String>())
        }
        getByName("viewer") {
            setRoot("src/viewer")
            assets.setSrcDirs(listOf("src/viewer/assets"))
        }
        getByName("admin") {
            setRoot("src/admin")
            assets.setSrcDirs(listOf("src/admin/assets"))
        }
    }


    // شماره نسخه/برچسب هر build را از تاریخچه Git می‌سازد تا هر build برچسب
    // منحصربه‌فرد و قابل‌ردیابی داشته باشد؛ اگر پروژه از حالت git checkout نشده باشد
    // (مثلاً از یک فایل zip استخراج شده)، به مقدار ثابت پیش‌فرض برمی‌گردد تا build نشکند.
    val hasGit = rootProject.file(".git").exists()
    val gitCommitCount = if (hasGit) {
        runCatching {
            providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
                .standardOutput.asText.get().trim().toIntOrNull()
        }.getOrNull() ?: 1
    } else 1
    val gitShortSha = if (hasGit) {
        runCatching {
            providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }
                .standardOutput.asText.get().trim()
        }.getOrNull() ?: "local"
    } else "local"

    val ciReleaseNumber = providers.gradleProperty("releaseNumber").orNull?.toIntOrNull()
    val effectiveBuildNumber = ciReleaseNumber ?: gitCommitCount

    defaultConfig {
        minSdk = 23
        targetSdk = 34
        // شماره نسخه/برچسب هر build به‌صورت خودکار از تاریخچه Git ساخته می‌شود
        // (تعداد کامیت‌ها = versionCode، و نام نسخه شامل هش کوتاه کامیت است)
        // تا هر build یک برچسب منحصربه‌فرد داشته باشد و قابل ردیابی باشد.
        versionCode = effectiveBuildNumber
        // شماره نسخه برای هر دو flavor یکسان می‌ماند؛ تفاوت فقط در سطح دسترسی است.
        versionName = "1.0-build$effectiveBuildNumber+$gitShortSha"
    }

    val storeFilePath = signingProp("RELEASE_STORE_FILE")
    val hasReleaseSigning = storeFilePath != null

    signingConfigs {
        // امضای ثابت فقط برای Debug/آزمایش CI است تا APK هر build بتواند
        // روی build آزمایشی قبلی همان flavor نصبِ بروزرسانی شود. این کلید
        // برای انتشار رسمی/Play Store نیست.
        create("testDebug") {
            storeFile = rootProject.file("ci/tazieh-test-signing.jks")
            storePassword = "tazieh-test-2026"
            keyAlias = "taziehTest"
            keyPassword = "tazieh-test-2026"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(storeFilePath!!)
                storePassword = signingProp("RELEASE_STORE_PASSWORD")
                keyAlias = signingProp("RELEASE_KEY_ALIAS")
                keyPassword = signingProp("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // امضای ثابت برای زنجیره بروزرسانی APKهای آزمایشی CI.
            signingConfig = signingConfigs.getByName("testDebug")
        }
        release {
            // انتشار واقعی: کد Viewer در Release با R8 کوچک‌سازی و مبهم‌سازی می‌شود.
            isMinifyEnabled = true
            isShrinkResources = true
            // اگر کلید امضا تنظیم نشده باشد (مثلاً روی CI بدون secret)، بدون امضا
            // ساخته می‌شود تا Build نشکند؛ چنین APK ای فقط برای تست داخلی قابل‌نصب است،
            // نه انتشار در فروشگاه. برای انتشار واقعی، local.properties را طبق
            // DEVELOPER_GUIDE.md پر کنید.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // Room (database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // تست‌های واحد (اجرا روی JVM با Robolectric، بدون نیاز به شبیه‌ساز/گوشی)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
