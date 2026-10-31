package com.autohub.park.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/**
 * Uygulama içi çok dilli destek (i18n).
 * Anahtar = Türkçe kaynak metin; çeviriler assets/i18n/<kod>.json dosyalarından yüklenir.
 */
object I18n {

    data class Language(val code: String, val nativeLabel: String)

    val languages = listOf(
        Language("tr", "Türkçe"),
        Language("en", "English"),
        Language("es", "Español"),
        Language("pt", "Português"),
        Language("fr", "Français"),
        Language("de", "Deutsch"),
        Language("it", "Italiano"),
        Language("ru", "Русский"),
        Language("ar", "العربية"),
        Language("hi", "हिन्दी"),
        Language("zh", "中文")
    )

    private const val PREFS = "autolounge_i18n"

    var lang: String by mutableStateOf("tr")
        private set

    private val maps = ConcurrentHashMap<String, Map<String, String>>()
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("lang", null)
        lang = if (saved != null && languages.any { it.code == saved }) {
            saved
        } else {
            val device = java.util.Locale.getDefault().language.lowercase()
            if (languages.any { it.code == device }) device else "en"
        }
        // Çeviri haritası ilk ekran çizilmeden hazır olsun (yoksa açılışta Türkçe görünür)
        if (lang != "tr") mapFor(lang)
    }

    fun setLang(context: Context, code: String) {
        if (languages.none { it.code == code }) return
        lang = code
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("lang", code).apply()
        if (code != "tr") mapFor(code)
    }

    private fun mapFor(code: String): Map<String, String> {
        if (code == "tr") return emptyMap()
        maps[code]?.let { return it }
        val ctx = appContext ?: return emptyMap()
        val m = try {
            val text = ctx.assets.open("i18n/$code.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(text)
            val out = HashMap<String, String>(obj.length())
            obj.keys().forEach { k -> out[k] = obj.optString(k) }
            out
        } catch (_: Exception) {
            emptyMap()
        }
        maps[code] = m
        return m
    }

    /** Anahtarı geçerli dile çevirir. Çeviri yoksa Türkçe (anahtar) döner. */
    fun tr(key: String): String {
        val code = lang
        if (code == "tr") return key
        return mapFor(code)[key] ?: key
    }

    /** Parametreli çeviri: anahtar %s/%d içerir, String.format uygulanır. */
    fun tr(key: String, vararg args: Any): String = try {
        String.format(tr(key), *args)
    } catch (_: Exception) {
        tr(key)
    }

    val isRtl: Boolean get() = lang == "ar"
}

/** Üst düzey yardımcı fonksiyon: doğrudan tr("Anahtar") olarak kullanılabilir */
fun tr(key: String): String = I18n.tr(key)

/** Parametreli üst düzey yardımcı fonksiyon: tr("Kalan %d dakika", 15) */
fun tr(key: String, vararg args: Any): String = I18n.tr(key, *args)

