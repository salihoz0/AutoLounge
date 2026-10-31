package com.autolounge.park.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.autolounge.park.i18n.tr

/**
 * Android Auto kurulum kontrolleri.
 * Araç ekranında tam ekran (projeksiyon) çalışmak için Android Auto, uygulamanın
 * "Play Store tarafından yüklendi" görünmesini ister. "Bilinmeyen kaynaklar" yalnızca
 * resmi uygulama türlerini (medya/mesaj) açar; bu yüzden ikisini ayrı ayrı kontrol ediyoruz.
 */
object CarSetupChecker {

    const val AA_PACKAGE = "com.google.android.projection.gearhead"
    private const val PLAY_PACKAGE = "com.android.vending"
    private const val SHELL_PACKAGE = "com.android.shell"
    private const val PREFS = "autolounge_car_setup"
    private const val KEY_SESSION_SEEN = "car_session_seen"
    private const val KEY_UNKNOWN_SOURCES = "unknown_sources_confirmed"
    private const val KEY_BANNER_HIDDEN = "banner_hidden"

    enum class InstallSource {
        /** Play Store damgası tam: Android Auto kabul eder (KingInstaller / gerçek Play). */
        PLAY,
        /** Play damgası var ama kurulumu adb başlatmış; çoğu sürümde çalışır, bazılarında yok sayılır. */
        PLAY_VIA_ADB,
        /** Play damgası yok: araç ekranında tam ekran açılmaz. */
        OTHER
    }

    enum class PhoneBrand { SAMSUNG, PIXEL, XIAOMI, OTHER }

    data class Status(
        val androidVersion: String,
        val sdkInt: Int,
        val brand: PhoneBrand,
        val androidAutoInstalled: Boolean,
        val androidAutoVersion: String?,
        val installSource: InstallSource,
        val installerName: String?,
        val unknownSourcesConfirmed: Boolean,
        val carSessionSeen: Boolean
    ) {
        /**
         * Android 15+: Android Auto uygulamayı "park halinde uygulama" olarak doğrudan açar;
         * Play damgası gerekmez, yalnızca "Bilinmeyen kaynaklar" yeterlidir.
         */
        val usesParkedApps: Boolean get() = sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        /** Android 14'te KingInstaller'ın klasik yöntemi çalışmıyor; Shizuku gerekir. */
        val needsModernInstaller: Boolean get() = sdkInt == Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val isReady: Boolean get() = carSessionSeen ||
            (androidAutoInstalled && (usesParkedApps || installSource == InstallSource.PLAY))
    }

    fun check(context: Context): Status {
        val pm = context.packageManager
        val prefs = prefs(context)
        val aaVersion = try {
            pm.getPackageInfo(AA_PACKAGE, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val aaEnabled = aaVersion != null && try {
            pm.getApplicationInfo(AA_PACKAGE, 0).enabled
        } catch (_: Exception) {
            false
        }

        var installing: String? = null
        var initiating: String? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(context.packageName)
                installing = info.installingPackageName
                initiating = info.initiatingPackageName
            } else {
                @Suppress("DEPRECATION")
                installing = pm.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) {
        }
        val source = when {
            installing != PLAY_PACKAGE -> InstallSource.OTHER
            initiating == SHELL_PACKAGE -> InstallSource.PLAY_VIA_ADB
            else -> InstallSource.PLAY
        }

        return Status(
            androidVersion = Build.VERSION.RELEASE ?: "?",
            sdkInt = Build.VERSION.SDK_INT,
            brand = detectBrand(),
            androidAutoInstalled = aaEnabled,
            androidAutoVersion = aaVersion,
            installSource = source,
            installerName = installing,
            unknownSourcesConfirmed = prefs.getBoolean(KEY_UNKNOWN_SOURCES, false),
            carSessionSeen = prefs.getBoolean(KEY_SESSION_SEEN, false)
        )
    }

    private fun detectBrand(): PhoneBrand {
        val m = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        return when {
            "samsung" in m -> PhoneBrand.SAMSUNG
            "google" in m -> PhoneBrand.PIXEL
            "xiaomi" in m || "redmi" in m || "poco" in m -> PhoneBrand.XIAOMI
            else -> PhoneBrand.OTHER
        }
    }

    /** Telefon markasına göre Android Auto ayarlarının elle bulunacağı yol. */
    fun androidAutoSettingsPath(brand: PhoneBrand): String = when (brand) {
        PhoneBrand.SAMSUNG -> tr("Ayarlar → Bağlı cihazlar → Android Auto")
        PhoneBrand.PIXEL -> tr("Ayarlar → Bağlı cihazlar → Bağlantı tercihleri → Android Auto")
        PhoneBrand.XIAOMI -> tr("Ayarlar → Bağlantı ve paylaşım → Android Auto")
        PhoneBrand.OTHER -> tr("Ayarlar → arama kutusuna \"Android Auto\" yazın")
    }

    /** Araç ekranı bir kez açıldığında çağrılır: kurulumun gerçekten çalıştığının kanıtı. */
    fun markCarSessionSeen(context: Context) {
        prefs(context).edit().putBoolean(KEY_SESSION_SEEN, true).apply()
    }

    fun setUnknownSourcesConfirmed(context: Context, confirmed: Boolean) {
        prefs(context).edit().putBoolean(KEY_UNKNOWN_SOURCES, confirmed).apply()
    }

    fun isBannerHidden(context: Context): Boolean = prefs(context).getBoolean(KEY_BANNER_HIDDEN, false)

    fun hideBanner(context: Context) {
        prefs(context).edit().putBoolean(KEY_BANNER_HIDDEN, true).apply()
    }

    /** Android Auto ayarlarını doğrudan açar; olmazsa uygulama bilgisi sayfasına düşer. */
    fun openAndroidAutoSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("$AA_PACKAGE.SETTINGS").setPackage(AA_PACKAGE),
            Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(AA_PACKAGE),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$AA_PACKAGE"))
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }

    fun openAndroidAutoInPlayStore(context: Context) {
        openUrl(context, "market://details?id=$AA_PACKAGE", "https://play.google.com/store/apps/details?id=$AA_PACKAGE")
    }

    fun openUrl(context: Context, url: String, fallback: String? = null) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            if (fallback != null) openUrl(context, fallback)
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
