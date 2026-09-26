# انتشار خودکار نسخه‌ی جدید (Release and Publish Update)

این مرحله ساخت APK نهایی (امضاشده با کلید Release واقعی)، ساخت `update.json` هر دو
Flavor، و push کردن هر دو به ریپازیتوری جدای سرور بروزرسانی را در یک workflow
دستی (`workflow_dispatch`) خودکار می‌کند: `.github/workflows/release.yml`.

این workflow **خودکار روی هر push اجرا نمی‌شود** — عمداً فقط دستی (از تب Actions
گیت‌هاب، دکمه‌ی "Run workflow") اجرا می‌شود، چون یک عمل انتشار واقعی است (امضای
Release + push به ریپازیتوری دیگر)، نه یک build آزمایشی.

## پیش‌نیاز ۱ — ریپازیتوری جدای سرور بروزرسانی

طبق توصیه‌ی امنیتی قبلی، یک ریپازیتوری **کاملاً جدا** بسازید (مثلاً `tazieh-updates`)
که هیچ ربطی به ریپازیتوری سورس اصلی نداشته باشد و Public باشد، با این ساختار اولیه:

```
tazieh-updates/
  admin/
  user/
```
(پوشه‌ها می‌توانند خالی باشند؛ workflow خودش فایل‌ها را داخلشان می‌سازد.)

## پیش‌نیاز ۲ — کلید امضای Release

اگر هنوز نساخته‌اید:
```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tazieh
```
سپس آن را به Base64 تبدیل کنید (برای گذاشتن در GitHub Secret):
```bash
base64 -w0 release-key.jks > release-key.b64.txt
```
محتوای همین فایل `.txt` را در مرحله‌ی بعد کپی می‌کنید.

## پیش‌نیاز ۳ — تنظیم Secrets و Variables در ریپازیتوری سورس

در ریپازیتوری سورس اصلی: **Settings → Secrets and variables → Actions**

### تب Secrets (مقادیر حساس):
| نام | مقدار |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | محتوای فایل `release-key.b64.txt` |
| `RELEASE_STORE_PASSWORD` | رمز keystore |
| `RELEASE_KEY_ALIAS` | `tazieh` (یا هر alias که ساختید) |
| `RELEASE_KEY_PASSWORD` | رمز کلید |
| `UPDATES_REPO_TOKEN` | یک Personal Access Token (زیر توضیح داده شده) |

**ساخت `UPDATES_REPO_TOKEN`:** یک Fine-grained PAT بسازید (GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens) که فقط روی ریپازیتوری `tazieh-updates` دسترسی `Contents: Read and write` داشته باشد — نه روی کل حساب کاربری، تا اگر این توکن لو رفت، هیچ دسترسی به سورس اصلی ندهد.

### تب Variables (مقادیر غیرحساس):
| نام | مثال مقدار |
|---|---|
| `UPDATES_REPO` | `USERNAME/tazieh-updates` |
| `UPDATE_BASE_URL_ADMIN` | `https://raw.githubusercontent.com/USERNAME/tazieh-updates/main/admin` |
| `UPDATE_BASE_URL_VIEWER` | `https://raw.githubusercontent.com/USERNAME/tazieh-updates/main/user` |

این دو آدرس باید همان مقادیری باشند که در `UPDATE_SERVER_ADMIN_URLS` / `UPDATE_SERVER_VIEWER_URLS` (در `local.properties` یا `update-servers.properties`) هم گذاشته‌اید، تا برنامه‌ی نصب‌شده روی گوشی‌ها همین آدرس‌ها را بررسی کند.

## اجرای انتشار

1. به تب **Actions** ریپازیتوری سورس بروید.
2. workflow با نام **"Release and Publish Update"** را انتخاب کنید.
3. دکمه‌ی **"Run workflow"** را بزنید؛ اگر خواستید، «تغییرات این نسخه» را تایپ کنید (هر خط یک مورد، برای هر دو Admin و Viewer یکسان اعمال می‌شود).
4. اجرا که تمام شد:
   - APK امضاشده‌ی هر دو Flavor ساخته می‌شود (با `versionCode` برابر شماره‌ی اجرای این workflow).
   - `update.json` هر دو از روی همان APK واقعی ساخته می‌شود (packageName/versionCode/versionName/SHA-256 مستقیم از فایل خوانده می‌شود، نه دستی).
   - هر دو APK + `update.json` به ریپازیتوری `tazieh-updates` commit و push می‌شوند.
   - یک نسخه‌ی پشتیبان از APKها هم به‌عنوان Artifact همان workflow run آپلود می‌شود (برای نصب دستی روی گوشی خودتان، چون این‌ها را باید یک‌بار خودتان هم نصب کنید).

از این لحظه، هر کاربری که «تنظیمات → بررسی بروزرسانی برنامه» را بزند، نسخه‌ی جدید را می‌بیند.

## نکات مهم

- اگر می‌خواهید این نسخه را روی گوشی خودتان هم نصب کنید، APK را از بخش **Artifacts** همان اجرای workflow دانلود کنید (چون امضایش با کلید Release واقعی است، اگر قبلاً نسخه‌ای با همین کلید نصب کرده باشید، جایگزین می‌شود نه پاک‌سازی).
- ابزار داخل اپ («تنظیمات → ساخت فایل update.json») همچنان برای زمانی که بخواهید بدون CI، دستی و محلی یک بروزرسانی منتشر کنید باقی می‌ماند — این workflow جایگزینش نمی‌کند، فقط مسیر خودکار اضافه‌ای است.
- اگر یکی از Secrets/Variables تنظیم نشده باشد، workflow همان ابتدا با یک پیام واضح متوقف می‌شود (به‌جای شکست مبهم در وسط build).
