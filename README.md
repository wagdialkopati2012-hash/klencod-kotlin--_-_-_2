# نظام المناقصات (Munakasat)

تطبيق Android (Kotlin + Jetpack Compose + Room) لإدارة عروض الأسعار والمناقصات والأصناف والتقارير.

## التشغيل
1. افتح المشروع في Android Studio ثم انتظر مزامنة Gradle.
2. شغّل التطبيق على محاكي أو جهاز (`app` → Run).
3. من سطر الأوامر: `./gradlew assembleDebug` (إن لم يوجد `gradle-wrapper.jar` نفّذ `gradle wrapper` مرة واحدة).

## ملاحظات
- `debug.keystore` مولَّد لتوقيع نسخة debug (كلمة المرور `android`).
- لتوقيع release عرّف المتغيرات `KEYSTORE_PATH` و`STORE_PASSWORD` و`KEY_PASSWORD`.
- ملف `.env` اختياري (انظر `.env.example`).
