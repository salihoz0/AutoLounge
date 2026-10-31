# ==============================================================================
# AutoLounge - ProGuard / R8 Yapılandırma Kuralları
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Android Auto SDK & Araç Entegrasyonu (aauto-sdk.jar & Gearhead Projection)
# ------------------------------------------------------------------------------
# Android Auto (Gearhead), CarActivity ve CarActivityService sınıflarını
# reflection (yansıma) ile yükler. Bu sınıfların ve SDK bileşenlerinin
# isimlerinin veya metodlarının kırpılması/gizlenmesi araç ekranının çökmesine yol açar.
-keep class com.google.android.apps.auto.sdk.** { *; }
-keep interface com.google.android.apps.auto.sdk.** { *; }
-keep class com.google.android.gms.car.** { *; }
-keep interface com.google.android.gms.car.** { *; }
-keep class android.support.car.** { *; }
-keep interface android.support.car.** { *; }
-keep class com.autolounge.park.car.** { *; }
-dontwarn com.google.android.apps.auto.sdk.**
-dontwarn com.google.android.gms.car.**
-dontwarn android.support.car.**

# ------------------------------------------------------------------------------
# 2. WebView & JavaScript Bridge Arayüzleri
# ------------------------------------------------------------------------------
# WebView üzerinden çalışan YouTube / HTML5 video oynatıcı ve JS köprüsü için
# @JavascriptInterface anotasyonlu metodlar ve WebChromeClient bileşenleri korunmalıdır.
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void onShowCustomView(android.view.View, android.webkit.WebChromeClient$CustomViewCallback);
    public void onHideCustomView();
    public boolean onShowFileChooser(android.webkit.WebView, android.webkit.ValueCallback, android.webkit.WebChromeClient$FileChooserParams);
}

# ------------------------------------------------------------------------------
# 3. AndroidX Media3 / ExoPlayer (HLS, Ses, Oturum & UI)
# ------------------------------------------------------------------------------
# Canlı TV akışları (HLS), arka plan ses oynatımı (MediaSession) ve SurfaceView/TextureView
-keep class androidx.media3.exoplayer.** { *; }
-keep interface androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep interface androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.extractor.** { *; }
-dontwarn androidx.media3.**

# ------------------------------------------------------------------------------
# 4. Coil Image Loader (Logo ve Kapak Görselleri)
# ------------------------------------------------------------------------------
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ------------------------------------------------------------------------------
# 5. Veri Modelleri, Room Veritabanı & JSON Serileştirme
# ------------------------------------------------------------------------------
# AutoLoungeSyncBridge, Room Entities, DataStore ve JSON (channels.json) modelleri
-keep class com.autolounge.park.data.** { *; }
-keepclassmembers class com.autolounge.park.data.** { *; }

# Room SQLite Veritabanı
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Parcelable / Parcelize Sınıfları
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ------------------------------------------------------------------------------
# 6. Dagger / Hilt Dependency Injection
# ------------------------------------------------------------------------------
# AutoLoungeCarActivity içinde EntryPointAccessors ile dinamik çözülen bileşenler
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep interface * extends dagger.hilt.internal.GeneratedComponent
-keep @dagger.hilt.EntryPoint interface * { *; }
-keep class com.autolounge.park.car.AutoLoungeCarActivity$CarActivityEntryPoint { *; }
-dontwarn dagger.hilt.**

# ------------------------------------------------------------------------------
# 7. Google Play Billing Client
# ------------------------------------------------------------------------------
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ------------------------------------------------------------------------------
# 8. Kotlin Coroutines & DataStore Preferences
# ------------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class androidx.datastore.** { *; }
