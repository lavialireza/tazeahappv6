package com.example.bookapp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bookapp.R

private const val CHANNEL_ID = "new_content"
private const val NOTIFICATION_ID = 1001

/**
 * کانال اعلان (فقط لازم برای اندروید ۸ به بعد) برای خبر دادن محتوای تازه.
 * فراخوانی این تابع بی‌خطر و idempotent است (ساخت دوباره‌ی یک کانال موجود مشکلی ندارد).
 */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "محتوای تازه",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "اطلاع‌رسانی وقتی مجلس یا تعزیه‌ی تازه‌ای به برنامه اضافه می‌شود"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

/**
 * اعلان محلی (بدون هیچ سروری) که وقتی محتوای تازه اضافه شد نشان داده می‌شود.
 * چون از اندروید ۱۳ به بعد نمایش اعلان نیاز به اجازه‌ی صریح کاربر دارد، اگر
 * اجازه داده نشده باشد این تابع فقط بی‌صدا کاری نمی‌کند (کرش نمی‌کند).
 */
fun showNewContentNotification(context: Context, newItemsCount: Int) {
    ensureNotificationChannel(context)
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("محتوای تازه اضافه شد")
        .setContentText("$newItemsCount مورد تازه به برنامه اضافه شد. برای دیدن، «چه چیزی جدیده» را باز کنید.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    try {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    } catch (e: SecurityException) {
        // اجازه‌ی نمایش اعلان داده نشده؛ نادیده می‌گیریم (بی‌خطر)
    }
}
