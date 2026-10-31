package com.autolounge.park.data

import java.util.Locale

data class NewsChannel(
    val id: String,
    val title: String,
    val url: String,
    val countryCode: String,
    val countryName: String,
    val audioStreamUrl: String? = null,
    val category: String = "Genel",
    val logoUrl: String? = null,
    val logoAsset: String? = null,
    val isCustom: Boolean = false,
    val kind: String = "tv"
)

object NewsChannelsRepository {

    @Volatile
    private var bundledChannels: List<NewsChannel> = emptyList()
        set(value) { field = value; cache = null }

    /** bundledChannels'a bağlı türetilmiş veriler; bundledChannels atanınca sıfırlanır. */
    private class Cache(
        val supported: List<Pair<String, String>>,
        val allChannels: List<NewsChannel>,
        val mergedDistinct: List<NewsChannel>,
        val byId: Map<String, NewsChannel>,
        val byLowerTitle: Map<String, NewsChannel>,
        val searchable: List<Pair<NewsChannel, Triple<String, String, String>>>
    )

    @Volatile
    private var cache: Cache? = null

    private fun cache(): Cache = cache ?: synchronized(this) {
        cache ?: run {
            val bundled = bundledChannels
            val base = listOf(
                "TR" to "Türkiye",
                "US" to "Amerika Birleşik Devletleri",
                "GB" to "Birleşik Krallık",
                "DE" to "Almanya",
                "FR" to "Fransa",
                "IT" to "İtalya",
                "ES" to "İspanya",
                "AZ" to "Azerbaycan",
                "GLOBAL" to "Dünya / Global"
            )
            val dflt = defaultChannels
            val byId = HashMap<String, NewsChannel>()
            (dflt + bundled).forEach { byId.putIfAbsent(it.id, it) }
            val byTitle = HashMap<String, NewsChannel>()
            (bundled + dflt).forEach { byTitle.putIfAbsent(it.title.trim().lowercase(Locale.ROOT), it) }
            val search = (dflt + bundled).distinctBy { it.id }.map {
                it to Triple(it.title.lowercase(Locale.ROOT), it.category.lowercase(Locale.ROOT), it.countryName.lowercase(Locale.ROOT))
            }
            Cache(
                supported = (base + bundled.map { it.countryCode to it.countryName }.distinct()).distinctBy { it.first },
                allChannels = if (bundled.isNotEmpty()) bundled + dflt else dflt,
                mergedDistinct = (dflt + bundled).distinctBy { it.countryCode + "|" + it.title.lowercase(Locale.ROOT) },
                byId = byId,
                byLowerTitle = byTitle,
                searchable = search
            ).also { cache = it }
        }
    }

    @Volatile
    private var initStarted = false

    // Ülke tespiti (SIM/ağ) ve kullanıcının ülke seçimini saklamak için
    @Volatile
    private var appContext: android.content.Context? = null
    private const val COUNTRY_PREFS = "autolounge_country"
    private const val KEY_SELECTED_COUNTRY = "selected"

    fun initBundledChannelsAsync(context: android.content.Context) {
        if (initStarted) return
        initStarted = true
        val appContext = context.applicationContext
        this.appContext = appContext
        Thread({ runCatching { initBundledChannels(appContext) } }, "bundled-channels-init").start()
    }

    fun assetUri(assetPath: String): String = "file:///android_asset/$assetPath"

    fun logoModelFor(channel: NewsChannel): String? {
        val direct = channel.logoAsset?.let { assetUri(it) } ?: channel.logoUrl
        if (!direct.isNullOrBlank()) return direct

        val slug = when (channel.title.lowercase(Locale.ROOT).trim()) {
            "trt 1" -> "trt1"
            "trt haber" -> "trthaber"
            "trt spor" -> "trtspor"
            "trt spor yıldız", "trt spor yildiz" -> "trtsporyildiz"
            "atv" -> "atv"
            "show tv", "showtv" -> "showtv"
            "kanal d", "kanald" -> "kanald"
            "star tv", "startv" -> "startv"
            "now (fox)", "now", "now tv" -> "now"
            "tv8", "tv 8" -> "tv8"
            "kanal 7", "kanal7" -> "kanal7"
            "beyaz tv", "beyaztv" -> "beyaztv"
            "teve2", "teve 2" -> "teve2"
            "tlc", "tlc türkiye", "tlc turkiye" -> "tlc"
            "dmax", "dmax türkiye", "dmax turkiye" -> "dmax"
            "24 tv", "24tv" -> "24tv"
            "tv360", "360" -> "tv360"
            "tv4" -> "tv4"
            "euro d" -> "eurod"
            "a haber", "ahaber" -> "ahaber"
            "ntv" -> "ntv"
            "habertürk", "haberturk", "habertürk tv", "haberturk tv" -> "haberturk"
            "cnn türk", "cnn turk" -> "cnnturk"
            "sözcü tv", "sözcü", "sozcu", "sozcu tv" -> "sozcu"
            "halk tv", "halktv" -> "halktv"
            "haber global", "haberglobal" -> "haberglobal"
            "bloomberg ht" -> "bloomberght"
            "tgrt haber", "tgrt" -> "tgrthaber"
            "tv100" -> "tv100"
            "tele1", "tele 1" -> "tele1"
            "ülke tv", "ulke tv" -> "ulketv"
            "ekotürk", "ekoturk" -> "ekoturk"
            "cnbc-e", "cnbce" -> "cnbce"
            "flash haber", "flash tv" -> "flashhaber"
            "tbmm tv", "tbmm" -> "tbmm"
            "bengü türk", "bengu turk" -> "benguturk"
            "krt tv", "krt" -> "krttv"
            "a spor", "aspor" -> "aspor"
            "fenerbahçe tv", "fenerbahce tv", "fb tv", "fbtv" -> "fbtv"
            "sports tv", "sportstv" -> "sportstv"
            "ht spor", "htspor" -> "htspor"
            "bein sports haber", "bein sports", "beinsports" -> "beinsports"
            "trt belgesel" -> "trtbelgesel"
            "trt çocuk", "trt cocuk" -> "trtcocuk"
            "minika çocuk", "minika cocuk" -> "minikacocuk"
            "minika go" -> "minikago"
            "kral pop tv", "kral pop" -> "kralpop"
            "kral tv" -> "kraltv"
            "power türk tv", "powertürk tv", "power turk tv", "powerturk tv" -> "powerturk"
            "power tv", "powertv" -> "powertv"
            "dream türk", "dream turk" -> "dreamturk"
            "trt müzik", "trt muzik" -> "trtmuzik"
            "number 1 tv", "nr1 tv", "number one tv" -> "numberone"
            "number 1 türk tv", "number 1 turk tv", "nr1 türk tv" -> "numberoneturk"
            "trt 2" -> "trt2"
            "trt avaz" -> "trtavaz"
            "trt türk", "trt turk" -> "trtturk"
            "power fm" -> "powerfm"
            "power türk", "power turk", "powertürk", "powerturk", "powertürk fm", "powerturk fm", "power türk fm", "power turk fm" -> "powerturk"
            "power pop", "powerpop" -> "powerpop"
            "kral fm" -> "kralfm"
            "slow türk", "slow turk" -> "slowturk"
            "süper fm", "super fm" -> "superfm"
            "joy türk", "joy turk" -> "joyturk"
            "trt fm" -> "trtfm"
            "trt türkü", "trt turku" -> "trtturku"
            "trt radyo 1", "trt radyo" -> "trtradyo1"
            "alem fm" -> "alemfm"
            "best fm" -> "bestfm"
            "show radyo" -> "showradyo"
            "radyo d" -> "radyod"
            "virgin radio", "virgin radio türkiye" -> "virginradio"
            // Global
            "bbc news world", "bbc news", "bbc news channel" -> "bbcnews"
            "cnn international", "cnn" -> "cnn"
            "al jazeera english", "al jazeera" -> "aljazeera"
            "euronews english", "euronews" -> "euronews"
            "deutsche welle", "dw", "dw english", "deutsche welle (dw english)" -> "dw"
            "france 24 english", "france 24", "france 24 français", "france 24 francais" -> "france24"
            "bloomberg television", "bloomberg tv", "bloomberg" -> "bloombergtv"
            "bloomberg radio" -> "bloombergradio"
            "trt world" -> "trtworld"
            "cgtn" -> "cgtn"
            "nhk world-japan", "nhk world" -> "nhkworld"
            "cna", "channel newsasia", "channel news asia" -> "cna"
            "arirang tv", "arirang" -> "arirang"
            "wion", "wion news" -> "wion"

            // Azerbaycan (AZ)
            "aztv" -> "aztv"
            "i̇ctimai tv (itv)", "ictimai tv (itv)", "i̇ctimai tv", "ictimai tv", "itv" -> "itv"
            "cbc sport azerbaijan", "cbc sport" -> "cbcsport"
            "xezer tv", "xəzər tv", "xezer" -> "xezertv"
            "space tv" -> "spacetv"
            "arb tv", "arb" -> "arbtv"
            "baku tv" -> "bakutv"
            "real tv" -> "realtv"
            "i̇dman tv", "idman tv", "idman azerbaycan" -> "idmantv"
            "mədəniyyət tv", "medeniyyet tv" -> "medeniyyettv"

            // Almanya (DE)
            "tagesschau24 (ard)", "tagesschau24", "tagesschau" -> "tagesschau24"
            "das erste (ard)", "das erste", "ard" -> "daserste"
            "zdf live", "zdf" -> "zdf"
            "zdfinfo", "zdf info" -> "zdfinfo"
            "n-tv nachrichten", "n-tv", "ntv de" -> "ntvde"
            "welt tv", "welt" -> "welt"
            "rtl television", "rtl", "rtl de" -> "rtl"
            "prosieben", "pro 7" -> "prosieben"
            "sat.1", "sat1", "sat 1" -> "sat1"
            "vox" -> "vox"
            "kika" -> "kika"
            "3sat", "dreisat" -> "dreisat"
            "arte deutsch", "arte" -> "arte"
            "phoenix" -> "phoenix"
            "antenne bayern" -> "antennebayern"
            "1live", "einslive" -> "einslive"
            "dlf", "deutschlandfunk" -> "dlf"
            "swr3", "swr 3" -> "swr3"

            // Birleşik Krallık (GB / UK)
            "sky news uk", "sky news" -> "skynews"
            "bbc one" -> "bbcone"
            "bbc two" -> "bbctwo"
            "itv1", "itv 1" -> "itv1"
            "itv2", "itv 2" -> "itv2"
            "channel 4", "channel4" -> "channel4"
            "channel 5", "channel5" -> "channel5"
            "gb news", "gbnews" -> "gbnews"
            "talktv", "talk tv" -> "talktv"
            "sky sports news" -> "skysportsnews"
            "capital fm", "capital" -> "capitalfm"
            "heart uk", "heart fm", "heart" -> "heartuk"
            "bbc radio 1", "bbc radio1" -> "bbcradio1"
            "bbc radio 2", "bbc radio2" -> "bbcradio2"
            "lbc", "lbc news" -> "lbc"
            "classic fm" -> "classicfm"

            // Amerika Birleşik Devletleri (US)
            "abc news live", "abc news" -> "abcnews"
            "cbs news 24/7", "cbs news" -> "cbsnews"
            "nbc news now", "nbc news" -> "nbcnews"
            "livenow from fox", "livenow fox", "livenow" -> "livenowfox"
            "scripps news" -> "scrippsnews"
            "cheddar news", "cheddar" -> "cheddarnews"
            "nasa tv", "nasa" -> "nasatv"
            "weathernation", "weather nation" -> "weathernation"
            "fox news live", "fox news" -> "foxnews"
            "msnbc" -> "msnbc"
            "pbs newshour", "pbs" -> "pbs"
            "cnbc us", "cnbc" -> "cnbc"
            "the weather channel" -> "weatherchannel"
            "c-span", "cspan" -> "cspan"
            "npr", "npr news" -> "npr"
            "fox sports radio" -> "foxsportsradio"
            "espn radio" -> "espnradio"

            // Fransa (FR)
            "bfm tv", "bfmtv" -> "bfmtv"
            "cnews" -> "cnews"
            "lci" -> "lci"
            "franceinfo", "france info" -> "franceinfo"
            "tv5monde", "tv5 monde" -> "tv5monde"
            "arte fr", "arte français", "arte francais" -> "artefr"
            "nrj" -> "nrj"
            "skyrock" -> "skyrock"
            "france inter" -> "franceinter"
            "rtl fr", "rtl france" -> "rtlfr"

            // İtalya (IT)
            "rai news 24", "rainews 24", "rainews24" -> "rainews24"
            "tgcom24", "tgcom 24" -> "tgcom24"
            "sky tg24", "skytg24" -> "skytg24"
            "rtl 102.5", "rtl 1025" -> "rtl1025"
            "rai 1", "rai1" -> "rai1"
            "radio 105", "radio105" -> "radio105"
            "rds" -> "rds"
            "radio deejay", "radiodeejay", "deejay" -> "radiodeejay"

            // İspanya (ES)
            "rtve 24h", "canal 24 horas", "rtve 24 horas" -> "rtve24h"
            "la 1", "la1", "la 1 (tve)" -> "la1"
            "antena 3", "antena3" -> "antena3"
            "telecinco" -> "telecinco"
            "cuatro" -> "cuatro"
            "lasexta", "la sexta" -> "lasexta"
            "cadena ser" -> "cadenaser"
            "los 40", "los 40 principales", "los40" -> "los40"
            "cadena dial" -> "cadenadial"
            "onda cero" -> "ondacero"
            else -> null
        }
        if (slug != null) return BrandLogos.uri(slug)
        return BrandLogos.findLogoByUrlOrTitle(channel.title, channel.url)
    }

    @Synchronized
    fun initBundledChannels(context: android.content.Context) {
        if (bundledChannels.isNotEmpty()) return
        runCatching {
            val text = context.assets.open("channels.json").bufferedReader().use { it.readText() }
            val arr = org.json.JSONArray(text)
            val list = ArrayList<NewsChannel>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val rawTitle = o.optString("title")
                val stream = o.optString("stream")
                if (rawTitle.isBlank()) continue
                val title = if (rawTitle.contains("group-title=") || rawTitle.contains("like Gecko")) {
                    rawTitle.substringAfterLast(",").substringAfterLast("\"").trim()
                } else rawTitle.trim()
                if (title.isBlank() || title.contains("like Gecko") || (title.contains("BBC First") && title.contains("Turk"))) continue
                list.add(
                    NewsChannel(
                        id = "bundled_$i",
                        title = title,
                        url = o.optString("url").ifBlank { stream },
                        countryCode = o.optString("countryCode").uppercase(Locale.ROOT),
                        countryName = o.optString("country"),
                        audioStreamUrl = stream.ifBlank { null },
                        category = o.optString("category").ifBlank { "Genel" },
                        logoUrl = o.optString("logo").ifBlank { null },
                        logoAsset = o.optString("asset").ifBlank { null },
                        isCustom = false
                    )
                )
            }
            bundledChannels = list
        }
    }

    fun logoFor(title: String): String? {
        val ch = cache().byLowerTitle[title.trim().lowercase(Locale.ROOT)]
        if (ch != null) {
            val m = logoModelFor(ch)
            if (m != null) return m
        }
        return BrandLogos.findLogoByUrlOrTitle(title, "")
    }

    val allChannels: List<NewsChannel>
        get() = cache().allChannels

    val defaultChannels: List<NewsChannel> = listOf(
        // ==========================================
        // 🇹🇷 TÜRKİYE — ULUSAL & EĞLENCE KANALLARI
        // ==========================================
        NewsChannel(
            id = "tr_trt1",
            title = "TRT 1",
            url = "https://www.trt1.com.tr/canli-yayin",
            audioStreamUrl = "https://tv-trt1.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/trt1.webp"
        ),
        NewsChannel(
            id = "tr_atv",
            title = "ATV",
            url = "https://www.atv.com.tr/canli-yayin",
            audioStreamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/atv.webp"
        ),
        NewsChannel(
            id = "tr_kanald",
            title = "Kanal D",
            url = "https://www.kanald.com.tr/canli-yayin",
            audioStreamUrl = "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/kanald.webp"
        ),
        NewsChannel(
            id = "tr_showtv",
            title = "Show TV",
            url = "https://www.showtv.com.tr/canli-yayin",
            audioStreamUrl = "https://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/showtv.webp"
        ),
        NewsChannel(
            id = "tr_startv",
            title = "Star TV",
            url = "https://www.startv.com.tr/canli-yayin",
            audioStreamUrl = "https://dogus.daioncdn.net/startv/startv_720p.m3u8?app=a20ac41e-bdc3-4aa1-934d-26b484480ac9&ce=3&sid=8l4w3lst4co5",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/startv.webp"
        ),
        NewsChannel(
            id = "tr_tv8",
            title = "TV8",
            url = "https://www.tv8.com.tr/canli-yayin",
            audioStreamUrl = "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/tv8.webp"
        ),
        NewsChannel(
            id = "tr_nowtv",
            title = "NOW (FOX)",
            url = "https://www.nowtv.com.tr/canli-yayin",
            audioStreamUrl = "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/now.webp"
        ),
        NewsChannel(
            id = "tr_kanal7",
            title = "Kanal 7",
            url = "https://www.kanal7.com/canli-izle",
            audioStreamUrl = "https://kanal7-live.daioncdn.net/kanal7/kanal7.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/kanal7.webp"
        ),
        NewsChannel(
            id = "tr_beyaztv",
            title = "Beyaz TV",
            url = "https://www.beyaztv.com.tr/canli-yayin/",
            audioStreamUrl = "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/beyaztv.webp"
        ),
        NewsChannel(
            id = "tr_teve2",
            title = "Teve2",
            url = "https://www.teve2.com.tr/canli-yayin",
            audioStreamUrl = "https://live.duhnet.tv/S2/HLS_LIVE/teve2np/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Eğlence",
            logoAsset = "logos/brand/teve2.webp"
        ),
        NewsChannel(
            id = "tr_tlc",
            title = "TLC Türkiye",
            url = "https://www.tlctv.com.tr/canli-izle",
            audioStreamUrl = "https://dogus.daioncdn.net/tlc/tlc.m3u8?app=tlc_web",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Eğlence",
            logoAsset = "logos/brand/tlc.webp"
        ),
        NewsChannel(
            id = "tr_dmax",
            title = "DMAX Türkiye",
            url = "https://www.dmaxtv.com.tr/canli-izle",
            audioStreamUrl = "https://dogus.daioncdn.net/dmax/dmax.m3u8?app=dmax_web",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Belgesel",
            logoAsset = "logos/brand/dmax.webp"
        ),
        NewsChannel(
            id = "tr_tv4",
            title = "TV4",
            url = "https://www.tv4.com.tr/canli-yayin",
            audioStreamUrl = "https://turkmedya-live.ercdn.net/tv4/tv4.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Eğlence",
            logoAsset = "logos/brand/tv4.webp"
        ),
        NewsChannel(
            id = "tr_eurod",
            title = "Euro D",
            url = "https://www.eurod.com.tr/canli-yayin",
            audioStreamUrl = "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/eurod.webp"
        ),

        // ==========================================
        // 🇹🇷 TÜRKİYE — HABER & EKONOMİ KANALLARI
        // ==========================================
        NewsChannel(
            id = "tr_trthaber",
            title = "TRT Haber",
            url = "https://www.trthaber.com/canli-yayin-izle.html",
            audioStreamUrl = "https://tv-trthaber.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/trthaber.webp"
        ),
        NewsChannel(
            id = "tr_haberturk",
            title = "Habertürk TV",
            url = "https://www.haberturk.com/canli-yayin",
            audioStreamUrl = "https://tv.ensonhaber.com/haberturk/haberturk.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/haberturk.webp"
        ),
        NewsChannel(
            id = "tr_ntv",
            title = "NTV",
            url = "https://www.ntv.com.tr/canli-yayin",
            audioStreamUrl = "https://dogus.daioncdn.net/ntv/ntv.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/ntv.webp"
        ),
        NewsChannel(
            id = "tr_cnnturk",
            title = "CNN Türk",
            url = "https://www.cnnturk.com/canli-yayin",
            audioStreamUrl = null,
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/cnnturk.webp"
        ),
        NewsChannel(
            id = "tr_sozcu",
            title = "Sözcü TV",
            url = "https://www.szctv.com.tr/canli-yayin-izle",
            audioStreamUrl = null,
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/sozcu.webp"
        ),
        NewsChannel(
            id = "tr_halktv",
            title = "Halk TV",
            url = "https://halktv.com.tr/canli-yayin",
            audioStreamUrl = "https://halktv-live.daioncdn.net/halktv/halktv.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/halktv.webp"
        ),
        NewsChannel(
            id = "tr_haberglobal",
            title = "Haber Global",
            url = "https://haberglobal.com.tr/canli-yayin",
            audioStreamUrl = "https://tv.ensonhaber.com/haberglobal/haberglobal.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/haberglobal.webp"
        ),
        NewsChannel(
            id = "tr_bloomberght",
            title = "Bloomberg HT",
            url = "https://www.bloomberght.com/canli-yayin",
            audioStreamUrl = "https://tv.ensonhaber.com/bloomberght/bloomberght.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ekonomi",
            logoAsset = "logos/brand/bloomberght.webp"
        ),
        NewsChannel(
            id = "tr_ahaber",
            title = "A Haber",
            url = "https://www.ahaber.com.tr/canli-yayin",
            audioStreamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/ahaber.webp"
        ),
        NewsChannel(
            id = "tr_tgrthaber",
            title = "TGRT Haber",
            url = "https://www.tgrthaber.com/canli",
            audioStreamUrl = "https://canli.tgrthaber.com/tgrt.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/tgrthaber.webp"
        ),
        NewsChannel(
            id = "tr_tv100",
            title = "TV100",
            url = "https://www.tv100.com/canli-yayin",
            audioStreamUrl = "https://tv.ensonhaber.com/tv100/tv100.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/tv100.webp"
        ),
        NewsChannel(
            id = "tr_tele1",
            title = "Tele1",
            url = "https://tele1.com.tr/canli-yayin/",
            audioStreamUrl = "https://tele1-live.ercdn.net/tele1/tele1_720p.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/tele1.webp"
        ),
        NewsChannel(
            id = "tr_ulketv",
            title = "Ülke TV",
            url = "https://www.ulketv.com/ulke-tv-canli-yayin",
            audioStreamUrl = "https://livetv.radyotvonline.net/kanal7live/ulketv/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/ulketv.webp"
        ),
        NewsChannel(
            id = "tr_ekoturk",
            title = "Ekotürk",
            url = "https://www.ekoturk.com/canli-yayin/",
            audioStreamUrl = "https://cdn.ekoturk.net/hls/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ekonomi",
            logoAsset = "logos/brand/ekoturk.webp"
        ),
        NewsChannel(
            id = "tr_24tv",
            title = "24 TV",
            url = "https://www.yirmidort.tv/canli-yayin",
            audioStreamUrl = "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/24tv.webp"
        ),
        NewsChannel(
            id = "tr_tv360",
            title = "TV360",
            url = "https://www.tv360.com.tr/canli-yayin",
            audioStreamUrl = "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ulusal",
            logoAsset = "logos/brand/tv360.webp"
        ),
        NewsChannel(
            id = "tr_cnbce",
            title = "CNBC-e",
            url = "https://www.cnbce.com/canli-yayin",
            audioStreamUrl = "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Ekonomi",
            logoAsset = "logos/brand/cnbce.webp"
        ),
        NewsChannel(
            id = "tr_flashhaber",
            title = "Flash Haber",
            url = "https://flashhabertv.com.tr/canli-yayin",
            audioStreamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtyycglqauzjhlive/broadcast_67c053c48829f.smil/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/flashhaber.webp"
        ),
        NewsChannel(
            id = "tr_tbmm",
            title = "TBMM TV",
            url = "https://www.tbmm.gov.tr/yayinlar/tbmm-tv",
            audioStreamUrl = "https://meclistv-live.ercdn.net/meclistv/meclistv.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/tbmm.webp"
        ),
        NewsChannel(
            id = "tr_benguturk",
            title = "Bengü Türk",
            url = "https://www.benguturk.com/canli-yayin",
            audioStreamUrl = null,
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/benguturk.webp"
        ),
        NewsChannel(
            id = "tr_krttv",
            title = "KRT TV",
            url = "https://www.krttv.com.tr/canli-yayin",
            audioStreamUrl = null,
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Haber",
            logoAsset = "logos/brand/krttv.webp"
        ),

        // ==========================================
        // 🇹🇷 TÜRKİYE — SPOR KANALLARI
        // ==========================================
        NewsChannel(
            id = "tr_trtspor",
            title = "TRT Spor",
            url = "https://www.trtspor.com.tr/canli-yayin-izle",
            audioStreamUrl = "https://tv-trtspor1.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/trtspor.webp"
        ),
        NewsChannel(
            id = "tr_trtsporyildiz",
            title = "TRT Spor Yıldız",
            url = "https://www.trtspor.com.tr/canli-yayin-izle/trt-spor-yildiz",
            audioStreamUrl = "https://tv-trtspor2.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/trtsporyildiz.webp"
        ),
        NewsChannel(
            id = "tr_aspor",
            title = "A Spor",
            url = "https://www.aspor.com.tr/canli-yayin",
            audioStreamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/aspor.webp"
        ),
        NewsChannel(
            id = "tr_fbtv",
            title = "Fenerbahçe TV",
            url = "https://www.fenerbahce.org/fbtv/canli-yayin",
            audioStreamUrl = "https://fbtv.fenerbahce.org/fenerbahcetv.stream/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/fbtv.webp"
        ),
        NewsChannel(
            id = "tr_sportstv",
            title = "Sports TV",
            url = "https://www.sportstv.com.tr/canli-yayin",
            audioStreamUrl = "https://ciner-live.ercdn.net/htspor/htspor.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/sportstv.webp"
        ),
        NewsChannel(
            id = "tr_htspor",
            title = "HT Spor",
            url = "https://www.htspor.com/canli-yayin",
            audioStreamUrl = "https://ciner-live.ercdn.net/htspor/htspor.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/htspor.webp"
        ),
        NewsChannel(
            id = "tr_beinsports",
            title = "beIN Sports Haber",
            url = "https://beinsports.com.tr/canli-yayin",
            audioStreamUrl = null,
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Spor",
            logoAsset = "logos/brand/beinsports.webp"
        ),

        // ==========================================
        // 🇹🇷 TÜRKİYE — BELGESEL & ÇOCUK & MÜZİK
        // ==========================================
        NewsChannel(
            id = "tr_trtbelgesel",
            title = "TRT Belgesel",
            url = "https://www.trtbelgesel.com.tr/canli-izle",
            audioStreamUrl = "https://tv-trtbelgesel.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Belgesel",
            logoAsset = "logos/brand/trtbelgesel.webp"
        ),
        NewsChannel(
            id = "tr_trtcocuk",
            title = "TRT Çocuk",
            url = "https://www.trtcocuk.net.tr/canli-izle",
            audioStreamUrl = "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Çocuk",
            logoAsset = "logos/brand/trtcocuk.webp"
        ),
        NewsChannel(
            id = "tr_minikacocuk",
            title = "Minika Çocuk",
            url = "https://www.minikacocuk.com.tr/canli-yayin",
            audioStreamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Çocuk",
            logoAsset = "logos/brand/minikacocuk.webp"
        ),
        NewsChannel(
            id = "tr_minikago",
            title = "Minika GO",
            url = "https://www.minikago.com.tr/canli-yayin",
            audioStreamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Çocuk",
            logoAsset = "logos/brand/minikago.webp"
        ),
        NewsChannel(
            id = "tr_kralpop",
            title = "Kral Pop TV",
            url = "https://www.kralmuzik.com.tr/tv/kral-pop-tv",
            audioStreamUrl = "https://dygedge.radyotvonline.net/kralpoptv/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/kralpop.webp"
        ),
        NewsChannel(
            id = "tr_kraltv",
            title = "Kral TV",
            url = "https://www.kralmuzik.com.tr/tv/kral-tv",
            audioStreamUrl = "https://dygedge.radyotvonline.net/kralpoptv/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/kraltv.webp"
        ),
        NewsChannel(
            id = "tr_powerturk",
            title = "PowerTürk TV",
            url = "https://www.powerapp.com.tr/powerturk-tv/",
            audioStreamUrl = "https://livetv.powerapp.com.tr/powerturkTV/powerturkhd.smil/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/powerturk.webp"
        ),
        NewsChannel(
            id = "tr_powertv",
            title = "Power TV",
            url = "https://www.powerapp.com.tr/power-tv/",
            audioStreamUrl = "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/powertv.webp"
        ),
        NewsChannel(
            id = "tr_dreamturk",
            title = "Dream Türk",
            url = "https://www.dreamturk.com.tr/canli-yayin",
            audioStreamUrl = "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/dreamturk.webp"
        ),
        NewsChannel(
            id = "tr_trtmuzik",
            title = "TRT Müzik",
            url = "https://www.trtizle.com/canli/tv/trt-muzik",
            audioStreamUrl = "https://tv-trtmuzik.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/trtmuzik.webp"
        ),
        NewsChannel(
            id = "tr_numberone",
            title = "Number 1 TV",
            url = "https://www.numberone.com.tr/n1tv/",
            audioStreamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17cd59e8b.smil/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/numberone.webp"
        ),
        NewsChannel(
            id = "tr_numberoneturk",
            title = "Number 1 Türk TV",
            url = "https://www.numberone.com.tr/n1turktv/",
            audioStreamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e18f9cea15_1/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Müzik",
            logoAsset = "logos/brand/numberoneturk.webp"
        ),
        NewsChannel(
            id = "tr_trt2",
            title = "TRT 2",
            url = "https://www.trt2.com.tr/canli-izle",
            audioStreamUrl = "https://tv-trt2.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Kültür",
            logoAsset = "logos/brand/trt2.webp"
        ),
        NewsChannel(
            id = "tr_trtavaz",
            title = "TRT Avaz",
            url = "https://www.trtavaz.com.tr/canli-izle",
            audioStreamUrl = "https://tv-trtavaz.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Kültür",
            logoAsset = "logos/brand/trtavaz.webp"
        ),
        NewsChannel(
            id = "tr_trtturk",
            title = "TRT Türk",
            url = "https://www.trtturk.com.tr/canli-izle",
            audioStreamUrl = "https://tv-trtturk.medya.trt.com.tr/master.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Kültür",
            logoAsset = "logos/brand/trtturk.webp"
        ),

        // ==========================================
        // 📻 TÜRKİYE — DİJİTAL CANLI RADYOLAR
        // ==========================================
        NewsChannel(
            id = "tr_radio_powerfm",
            title = "Power FM",
            url = "https://www.powerapp.com.tr/powerfm/",
            audioStreamUrl = "https://listen.powerapp.com.tr/powerfm/256/chunks.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/powerfm.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_powerturkfm",
            title = "PowerTürk FM",
            url = "https://www.powerapp.com.tr/powerturk/",
            audioStreamUrl = "https://live.powerapp.com.tr/powerturk/abr/powerturk/128/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/powerturk.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_kralfm",
            title = "Kral FM",
            url = "https://www.kralmuzik.com.tr/radyo/kral-fm",
            audioStreamUrl = "https://dygedge2.radyotvonline.net/kralfm/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/kralfm.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_slowturk",
            title = "Slow Türk",
            url = "https://www.slowturk.com.tr/",
            audioStreamUrl = "https://radyo.duhnet.tv/ak_dtvh_slowturk",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/slowturk.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_superfm",
            title = "Süper FM",
            url = "https://karnaval.com/radyolar/superfm",
            audioStreamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/SUPER_FM_SC",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/superfm.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_joyturk",
            title = "Joy Türk",
            url = "https://karnaval.com/radyolar/joyturk",
            audioStreamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/JOY_TURK_SC",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/joyturk.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_trtfm",
            title = "TRT FM",
            url = "https://www.trtdinle.com/channel/trt-fm",
            audioStreamUrl = "https://rd-trtfm.medya.trt.com.tr/master_128.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/trtfm.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_trtturku",
            title = "TRT Türkü",
            url = "https://www.trtdinle.com/channel/trt-turku",
            audioStreamUrl = "https://rd-trtturku.medya.trt.com.tr/master_128.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/trtturku.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_trtradyo1",
            title = "TRT Radyo 1",
            url = "https://www.trtdinle.com/channel/trt-radyo-1",
            audioStreamUrl = "https://rd-trtradyo1.medya.trt.com.tr/master_128.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/trtradyo1.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_alemfm",
            title = "Alem FM",
            url = "https://www.alemfm.com/",
            audioStreamUrl = "https://turkmedya.radyotvonline.net/alemfmaac",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/alemfm.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_bestfm",
            title = "Best FM",
            url = "https://www.bestfm.com.tr/",
            audioStreamUrl = "http://37.247.98.8/stream/166/",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/bestfm.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_showradyo",
            title = "Show Radyo",
            url = "https://showradyo.com.tr/",
            audioStreamUrl = "https://showradyo.radyotvonline.net/showradyoaac",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/showradyo.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_radyod",
            title = "Radyo D",
            url = "https://www.radyod.com/",
            audioStreamUrl = "https://live.duhnet.tv/S2/HLS_LIVE/radyodnp/playlist.m3u8",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/radyod.webp",
            kind = "radio"
        ),
        NewsChannel(
            id = "tr_radio_virgin",
            title = "Virgin Radio Türkiye",
            url = "https://karnaval.com/radyolar/virginradioturkiye",
            audioStreamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/VIRGIN_RADIO_SC",
            countryCode = "TR",
            countryName = "Türkiye",
            category = "Radyo",
            logoAsset = "logos/brand/virginradio.webp",
            kind = "radio"
        ),

        // ==========================================
        // 🌐 DÜNYA / GLOBAL KANALLAR
        // ==========================================
        NewsChannel(
            id = "global_bbc",
            title = "BBC News World",
            url = "https://www.bbc.com/news",
            audioStreamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/bbcnews.webp"
        ),
        NewsChannel(
            id = "global_cnn",
            title = "CNN International",
            url = "https://edition.cnn.com",
            audioStreamUrl = "https://tunein.streamguys1.com/cnn-new",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/cnn.webp"
        ),
        NewsChannel(
            id = "global_aljazeera",
            title = "Al Jazeera English",
            url = "https://www.aljazeera.com/live",
            audioStreamUrl = "https://live-hls-apps-aja-fa.getaj.net/AJA/01.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/aljazeera.webp"
        ),
        NewsChannel(
            id = "global_euronews",
            title = "Euronews English",
            url = "https://www.euronews.com/live",
            audioStreamUrl = "https://cdn-euronews.akamaized.net/live/eds/euronews-pl/26382/index.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/euronews.webp"
        ),
        NewsChannel(
            id = "global_dw",
            title = "Deutsche Welle (DW English)",
            url = "https://www.dw.com/en/live-tv/s-100825",
            audioStreamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/dw.webp"
        ),
        NewsChannel(
            id = "global_france24",
            title = "France 24 English",
            url = "https://www.france24.com/en/live",
            audioStreamUrl = "https://live.france24.com/hls/live/2037222-b/F24_AR_HI_HLS/master_5000.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/france24.webp"
        ),
        NewsChannel(
            id = "global_bloomberg",
            title = "Bloomberg Television",
            url = "https://www.bloomberg.com/live",
            audioStreamUrl = "https://bloomberg.com/media-manifest/streams/us.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Ekonomi",
            logoAsset = "logos/brand/bloomberg.webp"
        ),
        NewsChannel(
            id = "global_trtworld",
            title = "TRT World",
            url = "https://www.trtworld.com/live",
            audioStreamUrl = "https://tv-trtworld.medya.trt.com.tr/master.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/trtworld.webp"
        ),
        NewsChannel(
            id = "global_cgtn",
            title = "CGTN",
            url = "https://www.cgtn.com/live",
            audioStreamUrl = "https://news.cgtn.com/resource/live/english/cgtn-news.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/cgtn.webp"
        ),
        NewsChannel(
            id = "global_nhkworld",
            title = "NHK World-Japan",
            url = "https://www3.nhk.or.jp/nhkworld/en/live/",
            audioStreamUrl = "https://nhk.lls.pbs.org/index.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/nhkworld.webp"
        ),
        NewsChannel(
            id = "global_cna",
            title = "CNA (Channel NewsAsia)",
            url = "https://www.channelnewsasia.com/watch-listen-live",
            audioStreamUrl = "https://live1.mediadesk.al/cnatvlive.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/cna.webp"
        ),
        NewsChannel(
            id = "global_arirang",
            title = "Arirang TV",
            url = "https://www.arirang.com/live",
            audioStreamUrl = "http://amdlive-ch01.ctnd.com.edgesuite.net/arirang_3ch/smil:arirang_3ch.smil/playlist.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/arirang.webp"
        ),
        NewsChannel(
            id = "global_wion",
            title = "WION",
            url = "https://www.wionews.com/live-tv",
            audioStreamUrl = "http://vg-zeefta.akamaized.net/ptnr-yupptv/title-wion/v1/master/611d79b11b77e2f571934fd80ca1413453772ac7/20c3c0d9-0256-43fe-bca6-70fdd490b957/main.m3u8",
            countryCode = "GLOBAL",
            countryName = "Dünya / Global",
            category = "Haber",
            logoAsset = "logos/brand/wion.webp"
        ),

        // ==========================================
        // 🇦🇿 AZERBAYCAN KANALLARI
        // ==========================================
        NewsChannel(
            id = "az_aztv",
            title = "AzTV",
            url = "https://aztv.az/az/live",
            audioStreamUrl = "https://str.yodacdn.net/azertv/index.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Ulusal",
            logoAsset = "logos/brand/aztv.webp"
        ),
        NewsChannel(
            id = "az_ictimai",
            title = "İctimai TV (ITV)",
            url = "https://itv.az/live",
            audioStreamUrl = "https://stream.itv.az/rtmp:/radio/playlist.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Ulusal",
            logoAsset = "logos/brand/ictimaitv.webp"
        ),
        NewsChannel(
            id = "az_cbcsport",
            title = "CBC Sport",
            url = "https://cbcsport.az/live",
            audioStreamUrl = "http://stream.apa.tv/apastream/index.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Spor",
            logoAsset = "logos/brand/cbcsport.webp"
        ),
        NewsChannel(
            id = "az_xezertv",
            title = "Xezer TV",
            url = "https://www.xezerxeber.az/live",
            audioStreamUrl = "https://www.xezerxeber.az/stream/index.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Ulusal",
            logoAsset = "logos/brand/xezertv.webp"
        ),
        NewsChannel(
            id = "az_spacetv",
            title = "Space TV",
            url = "https://spacetv.az/live",
            audioStreamUrl = null,
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Ulusal",
            logoAsset = "logos/brand/spacetv.webp"
        ),
        NewsChannel(
            id = "az_arbtv",
            title = "ARB TV",
            url = "https://arbtv.az/live",
            audioStreamUrl = "https://janya-ayaztv.vgcdn.net/ptnr-WebApp/title-Ayaz_TV/v1/vglive-sk-934820/main.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Ulusal",
            logoAsset = "logos/brand/arbtv.webp"
        ),
        NewsChannel(
            id = "az_bakutv",
            title = "Baku TV",
            url = "https://baku.tv/live",
            audioStreamUrl = "https://rtmp.baku.tv/hls/bakutv.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Haber",
            logoAsset = "logos/brand/bakutv.webp"
        ),
        NewsChannel(
            id = "az_realtv",
            title = "Real TV",
            url = "https://realtv.az/live",
            audioStreamUrl = null,
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Haber",
            logoAsset = "logos/brand/realtv.webp"
        ),
        NewsChannel(
            id = "az_idmantv",
            title = "İdman TV",
            url = "https://idmantv.az/live",
            audioStreamUrl = null,
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Spor",
            logoAsset = "logos/brand/idmantv.webp"
        ),
        NewsChannel(
            id = "az_medeniyyettv",
            title = "Mədəniyyət TV",
            url = "https://medeniyyettv.az/az/live",
            audioStreamUrl = "https://str.yodacdn.net/medeniyyettele/index.m3u8",
            countryCode = "AZ",
            countryName = "Azerbaycan",
            category = "Kültür",
            logoAsset = "logos/brand/medeniyyettv.webp"
        ),

        // ==========================================
        // 🇩🇪 ALMANYA KANALLARI & RADYOLARI
        // ==========================================
        NewsChannel(
            id = "de_tagesschau",
            title = "Tagesschau24 (ARD)",
            url = "https://www.tagesschau.de/multimedia/livestreams",
            audioStreamUrl = "https://tagesschau.akamaized.net/hls/live/2020115/tagesschau/tagesschau_1/master.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Haber",
            logoAsset = "logos/brand/tagesschau.webp"
        ),
        NewsChannel(
            id = "de_daserste",
            title = "Das Erste (ARD)",
            url = "https://www.daserste.de/live/index.html",
            audioStreamUrl = "https://daserste-live.ard-mcdn.de/daserste/live/hls/int/master.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/daserste.webp"
        ),
        NewsChannel(
            id = "de_zdf",
            title = "ZDF Live",
            url = "https://www.zdf.de/live-tv",
            audioStreamUrl = "https://dash4.antik.sk/live/test_zdf_avc_25p/playlist.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/zdf.webp"
        ),
        NewsChannel(
            id = "de_zdfinfo",
            title = "ZDFinfo",
            url = "https://www.zdf.de/sender/zdfinfo/live-tv",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Haber",
            logoAsset = "logos/brand/zdfinfo.webp"
        ),
        NewsChannel(
            id = "de_ntv",
            title = "n-tv Nachrichten",
            url = "https://www.n-tv.de/livestream/",
            audioStreamUrl = "https://dash4.antik.sk/live/test_ntv_tizen/playlist.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Haber",
            logoAsset = "logos/brand/ntvde.webp"
        ),
        NewsChannel(
            id = "de_welt",
            title = "WELT TV",
            url = "https://www.welt.de/tv-programm-live-stream/",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Haber",
            logoAsset = "logos/brand/welt.webp"
        ),
        NewsChannel(
            id = "de_rtl",
            title = "RTL Television",
            url = "https://plus.rtl.de/live-tv",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/rtl.webp"
        ),
        NewsChannel(
            id = "de_prosieben",
            title = "ProSieben",
            url = "https://www.prosieben.de/livestream",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/prosieben.webp"
        ),
        NewsChannel(
            id = "de_sat1",
            title = "SAT.1",
            url = "https://www.sat1.de/livestream",
            audioStreamUrl = "https://dash4.antik.sk/live/test_sat_eins_gold_tizen/playlist.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/sat1.webp"
        ),
        NewsChannel(
            id = "de_vox",
            title = "VOX",
            url = "https://plus.rtl.de/live-tv/vox",
            audioStreamUrl = "https://dash4.antik.sk/live/test_vox_tizen/playlist.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/vox.webp"
        ),
        NewsChannel(
            id = "de_kika",
            title = "KiKA",
            url = "https://www.kika.de/live",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Çocuk",
            logoAsset = "logos/brand/kika.webp"
        ),
        NewsChannel(
            id = "de_3sat",
            title = "3sat",
            url = "https://www.3sat.de/live",
            audioStreamUrl = "https://dash4.antik.sk/live/test_drei_sat_tizen/playlist.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Kültür",
            logoAsset = "logos/brand/3sat.webp"
        ),
        NewsChannel(
            id = "de_arte",
            title = "Arte Deutsch",
            url = "https://www.arte.tv/de/live/",
            audioStreamUrl = "https://dash4.antik.sk/live/test_arte_avc_25p/playlist.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Kültür",
            logoAsset = "logos/brand/arte.webp"
        ),
        NewsChannel(
            id = "de_phoenix",
            title = "Phoenix",
            url = "https://www.phoenix.de/livestream.html",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Haber",
            logoAsset = "logos/brand/phoenix.webp"
        ),
        NewsChannel(
            id = "de_br",
            title = "BR Fernsehen",
            url = "https://www.br.de/fernsehen/live-stream/",
            audioStreamUrl = "https://mcdn.br.de/br/fs/bfs_sued/hls/de/master.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/br.webp"
        ),
        NewsChannel(
            id = "de_swr",
            title = "SWR Fernsehen",
            url = "https://www.swr.de/swraktuell/live/",
            audioStreamUrl = null,
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/swr.webp"
        ),
        NewsChannel(
            id = "de_wdr",
            title = "WDR Fernsehen",
            url = "https://www1.wdr.de/fernsehen/live/",
            audioStreamUrl = "https://wdrfs247.akamaized.net/hls/live/681509/wdr_msl4_fs247/index.m3u8",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Ulusal",
            logoAsset = "logos/brand/wdr.webp"
        ),
        NewsChannel(
            id = "de_antennebayern",
            title = "Antenne Bayern",
            url = "https://www.antenne.de/",
            audioStreamUrl = "http://s6-webradio.antenne.de/antenne",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Radyo",
            logoAsset = "logos/brand/antennebayern.webp"
        ),
        NewsChannel(
            id = "de_1live",
            title = "1LIVE",
            url = "https://www1.wdr.de/radio/1live/",
            audioStreamUrl = "http://wdr-1live-live.icecast.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Radyo",
            logoAsset = "logos/brand/einslive.webp"
        ),
        NewsChannel(
            id = "de_dlf",
            title = "Deutschlandfunk",
            url = "https://www.deutschlandfunk.de/",
            audioStreamUrl = "http://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Radyo",
            logoAsset = "logos/brand/dlf.webp"
        ),
        NewsChannel(
            id = "de_swr3",
            title = "SWR3",
            url = "https://www.swr3.de/",
            audioStreamUrl = "http://swr-swr3-live.cast.addradio.de/swr/swr3/live/mp3/128/stream.mp3",
            countryCode = "DE",
            countryName = "Almanya",
            category = "Radyo",
            logoAsset = "logos/brand/swr3.webp"
        ),

        // ==========================================
        // 🇬🇧 BİRLEŞİK KRALLIK KANALLARI & RADYOLARI
        // ==========================================
        NewsChannel(
            id = "gb_skynews",
            title = "Sky News UK",
            url = "https://news.sky.com/watch-live",
            audioStreamUrl = "https://distro001-gb-hls1-prd.delivery.skycdp.com/easel_cdn/ngrp:weather_loop.stream_all/playlist.m3u8",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Haber",
            logoAsset = "logos/brand/skynews.webp"
        ),
        NewsChannel(
            id = "gb_bbcnews",
            title = "BBC News Channel",
            url = "https://www.bbc.com/news",
            audioStreamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Haber",
            logoAsset = "logos/brand/bbcnews.webp"
        ),
        NewsChannel(
            id = "gb_bbcworldservice",
            title = "BBC World Service",
            url = "https://www.bbc.co.uk/worldserviceradio",
            audioStreamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Haber",
            logoAsset = "logos/brand/bbcnews.webp"
        ),
        NewsChannel(
            id = "gb_bbcone",
            title = "BBC One",
            url = "https://www.bbc.co.uk/iplayer/live/bbcone",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Ulusal",
            logoAsset = "logos/brand/bbcone.webp"
        ),
        NewsChannel(
            id = "gb_bbctwo",
            title = "BBC Two",
            url = "https://www.bbc.co.uk/iplayer/live/bbctwo",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Ulusal",
            logoAsset = "logos/brand/bbctwo.webp"
        ),
        NewsChannel(
            id = "gb_itv1",
            title = "ITV1",
            url = "https://www.itv.com/watch",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Ulusal",
            logoAsset = "logos/brand/itv1.webp"
        ),
        NewsChannel(
            id = "gb_itv2",
            title = "ITV2",
            url = "https://www.itv.com/watch",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Ulusal",
            logoAsset = "logos/brand/itv2.webp"
        ),
        NewsChannel(
            id = "gb_channel4",
            title = "Channel 4",
            url = "https://www.channel4.com/now/c4",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Ulusal",
            logoAsset = "logos/brand/channel4.webp"
        ),
        NewsChannel(
            id = "gb_channel5",
            title = "Channel 5",
            url = "https://www.channel5.com/",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Ulusal",
            logoAsset = "logos/brand/channel5.webp"
        ),
        NewsChannel(
            id = "gb_gbnews",
            title = "GB News",
            url = "https://www.gbnews.com/watch/live",
            audioStreamUrl = "https://cdn-apse1-prod.tsv2.amagi.tv/linear/amg01076-lightningintern-gbnewsnz-samsungnz/playlist.m3u8",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Haber",
            logoAsset = "logos/brand/gbnews.webp"
        ),
        NewsChannel(
            id = "gb_talktv",
            title = "TalkTV",
            url = "https://talk.tv/live",
            audioStreamUrl = "https://488f4ce4.wurl.com/master/f36d25e7e52f1ba8d7e56eb859c636563214f541/TEctZ2JfVGFsa19ITFM/playlist.m3u8",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Haber",
            logoAsset = "logos/brand/talktv.webp"
        ),
        NewsChannel(
            id = "gb_skysportsnews",
            title = "Sky Sports News",
            url = "https://www.skysports.com/watch",
            audioStreamUrl = null,
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Spor",
            logoAsset = "logos/brand/skysportsnews.webp"
        ),
        NewsChannel(
            id = "gb_capitalfm",
            title = "Capital FM",
            url = "https://www.capitalfm.com/",
            audioStreamUrl = "https://icecast.thisisdax.com/CapitalMP3",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Radyo",
            logoAsset = "logos/brand/capitalfm.webp"
        ),
        NewsChannel(
            id = "gb_heartuk",
            title = "Heart UK",
            url = "https://www.heart.co.uk/",
            audioStreamUrl = "https://icecast.thisisdax.com/HeartUKMP3",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Radyo",
            logoAsset = "logos/brand/heartuk.webp"
        ),
        NewsChannel(
            id = "gb_bbcradio1",
            title = "BBC Radio 1",
            url = "https://www.bbc.co.uk/sounds/play/live:bbc_radio_one",
            audioStreamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Radyo",
            logoAsset = "logos/brand/bbcradio1.webp"
        ),
        NewsChannel(
            id = "gb_bbcradio2",
            title = "BBC Radio 2",
            url = "https://www.bbc.co.uk/sounds/play/live:bbc_radio_two",
            audioStreamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_two",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Radyo",
            logoAsset = "logos/brand/bbcradio2.webp"
        ),
        NewsChannel(
            id = "gb_lbc",
            title = "LBC",
            url = "https://www.lbc.co.uk/",
            audioStreamUrl = "https://icecast.thisisdax.com/LBCUKMP3",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Radyo",
            logoAsset = "logos/brand/lbc.webp"
        ),
        NewsChannel(
            id = "gb_classicfm",
            title = "Classic FM",
            url = "https://www.classicfm.com/",
            audioStreamUrl = "https://icecast.thisisdax.com/ClassicFMMP3",
            countryCode = "GB",
            countryName = "Birleşik Krallık",
            category = "Radyo",
            logoAsset = "logos/brand/classicfm.webp"
        ),

        // ==========================================
        // 🇺🇸 AMERİKA BİRLEŞİK DEVLETLERİ KANALLARI & RADYOLARI
        // ==========================================
        NewsChannel(
            id = "us_abcnews",
            title = "ABC News Live",
            url = "https://abcnews.go.com/Live",
            audioStreamUrl = "https://abc-news-dmd-streams-1.akamaized.net/out/v1/701126012d044971b3fa89406a440133/index.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/abcnews.webp"
        ),
        NewsChannel(
            id = "us_cbsnews",
            title = "CBS News 24/7",
            url = "https://www.cbsnews.com/live/",
            audioStreamUrl = "https://jmp2.uk/plu-6350fdd266e9ea0007bedec5.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/cbsnews.webp"
        ),
        NewsChannel(
            id = "us_nbcnews",
            title = "NBC News NOW",
            url = "https://www.nbcnews.com/now",
            audioStreamUrl = "https://d1si3n1st4nkgb.cloudfront.net/10502/88896001/hls/master.m3u8?ads.xumo_channelId=88896001",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/nbcnews.webp"
        ),
        NewsChannel(
            id = "us_livenowfox",
            title = "LiveNOW from FOX",
            url = "https://www.livenowfox.com/",
            audioStreamUrl = "https://cdn-uw2-prod.tsv2.amagi.tv/linear/amg00488-foxdigital-livenowbyfox-lgus/playlist.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/livenowfox.webp"
        ),
        NewsChannel(
            id = "us_bloomberg",
            title = "Bloomberg TV US",
            url = "https://www.bloomberg.com/live/us",
            audioStreamUrl = "https://bloomberg.com/media-manifest/streams/us.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Ekonomi",
            logoAsset = "logos/brand/bloombergtv.webp"
        ),
        NewsChannel(
            id = "us_scrippsnews",
            title = "Scripps News",
            url = "https://scrippsnews.com/live/",
            audioStreamUrl = "https://jmp2.uk/plu-5459795fc9f133a519bc0bef.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/scrippsnews.webp"
        ),
        NewsChannel(
            id = "us_cheddarnews",
            title = "Cheddar News",
            url = "https://cheddar.com/",
            audioStreamUrl = "https://hls.livecdn.io/cheddar.com/cheddar/playlist.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Ekonomi",
            logoAsset = "logos/brand/cheddarnews.webp"
        ),
        NewsChannel(
            id = "us_nasatv",
            title = "NASA TV",
            url = "https://www.nasa.gov/live",
            audioStreamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Bilim",
            logoAsset = "logos/brand/nasatv.webp"
        ),
        NewsChannel(
            id = "us_weathernation",
            title = "WeatherNation",
            url = "https://www.weathernationtv.com/",
            audioStreamUrl = "https://d2ferbiwcx1539.cloudfront.net/v1/master/3722c60a815c199d9c0ef36c5b73da68a62b09d1/cc-8zd06wicndthf-ssai-prd/WNNationalSamsung/WNNationalSamsung.m3u8",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/weathernation.webp"
        ),
        NewsChannel(
            id = "us_foxnews",
            title = "FOX News Live",
            url = "https://www.foxnews.com/live-stream",
            audioStreamUrl = null,
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/foxnews.webp"
        ),
        NewsChannel(
            id = "us_msnbc",
            title = "MSNBC",
            url = "https://www.msnbc.com/live",
            audioStreamUrl = null,
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/msnbc.webp"
        ),
        NewsChannel(
            id = "us_pbs",
            title = "PBS NewsHour",
            url = "https://www.pbs.org/newshour/",
            audioStreamUrl = null,
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/pbs.webp"
        ),
        NewsChannel(
            id = "us_cnbc",
            title = "CNBC US",
            url = "https://www.cnbc.com/live-tv/",
            audioStreamUrl = null,
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Ekonomi",
            logoAsset = "logos/brand/cnbc.webp"
        ),
        NewsChannel(
            id = "us_weather",
            title = "The Weather Channel",
            url = "https://weather.com/",
            audioStreamUrl = null,
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/weatherchannel.webp"
        ),
        NewsChannel(
            id = "us_cspan",
            title = "C-SPAN",
            url = "https://www.c-span.org/networks/",
            audioStreamUrl = null,
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Haber",
            logoAsset = "logos/brand/cspan.webp"
        ),
        NewsChannel(
            id = "us_npr",
            title = "NPR News",
            url = "https://www.npr.org/",
            audioStreamUrl = "https://npr-ice.streamguys1.com/live.mp3",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Radyo",
            logoAsset = "logos/brand/npr.webp"
        ),
        NewsChannel(
            id = "us_foxsportsradio",
            title = "FOX Sports Radio",
            url = "https://foxsportsradio.iheart.com/",
            audioStreamUrl = "https://stream.revma.ihrhls.com/zc139",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Radyo",
            logoAsset = "logos/brand/foxsportsradio.webp"
        ),
        NewsChannel(
            id = "us_espnradio",
            title = "ESPN Radio",
            url = "https://www.espn.com/radio/",
            audioStreamUrl = "https://live.wostreaming.net/direct/espn-radioaac-ibc1",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Radyo",
            logoAsset = "logos/brand/espnradio.webp"
        ),
        NewsChannel(
            id = "us_cnnradio",
            title = "CNN Radio",
            url = "https://tunein.streamguys1.com/cnn-new",
            audioStreamUrl = "https://tunein.streamguys1.com/cnn-new",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Radyo",
            logoAsset = "logos/brand/cnn.webp"
        ),
        NewsChannel(
            id = "us_bloombergradio",
            title = "Bloomberg Radio",
            url = "https://www.bloomberg.com/audio",
            audioStreamUrl = "https://stream.revma.ihrhls.com/zc7281",
            countryCode = "US",
            countryName = "Amerika Birleşik Devletleri",
            category = "Radyo",
            logoAsset = "logos/brand/bloombergradio.webp"
        ),

        // ==========================================
        // 🇫🇷 FRANSA KANALLARI & RADYOLARI
        // ==========================================
        NewsChannel(
            id = "fr_france24",
            title = "France 24 Français",
            url = "https://www.france24.com/fr/direct",
            audioStreamUrl = "https://live.france24.com/hls/live/2037222-b/F24_AR_HI_HLS/master_5000.m3u8",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Haber",
            logoAsset = "logos/brand/france24.webp"
        ),
        NewsChannel(
            id = "fr_bfmtv",
            title = "BFM TV",
            url = "https://www.bfmtv.com/",
            audioStreamUrl = null,
            countryCode = "FR",
            countryName = "Fransa",
            category = "Haber",
            logoAsset = "logos/brand/bfmtv.webp"
        ),
        NewsChannel(
            id = "fr_cnews",
            title = "CNews",
            url = "https://www.cnews.fr/le-direct",
            audioStreamUrl = null,
            countryCode = "FR",
            countryName = "Fransa",
            category = "Haber",
            logoAsset = "logos/brand/cnews.webp"
        ),
        NewsChannel(
            id = "fr_lci",
            title = "LCI",
            url = "https://www.tf1info.fr/direct/",
            audioStreamUrl = null,
            countryCode = "FR",
            countryName = "Fransa",
            category = "Haber",
            logoAsset = "logos/brand/lci.webp"
        ),
        NewsChannel(
            id = "fr_franceinfo",
            title = "France Info",
            url = "https://www.francetvinfo.fr/en-direct/tv.html",
            audioStreamUrl = null,
            countryCode = "FR",
            countryName = "Fransa",
            category = "Haber",
            logoAsset = "logos/brand/franceinfo.webp"
        ),
        NewsChannel(
            id = "fr_tv5monde",
            title = "TV5Monde",
            url = "https://www.tv5monde.com/",
            audioStreamUrl = "https://ott.tv5monde.com/Content/HLS/Live/channel(fbs)/index.m3u8",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Genel",
            logoAsset = "logos/brand/tv5monde.webp"
        ),
        NewsChannel(
            id = "fr_arte",
            title = "Arte FR",
            url = "https://www.arte.tv/fr/direct/",
            audioStreamUrl = null,
            countryCode = "FR",
            countryName = "Fransa",
            category = "Kültür",
            logoAsset = "logos/brand/artefr.webp"
        ),
        NewsChannel(
            id = "fr_euronews",
            title = "Euronews Français",
            url = "https://fr.euronews.com/live",
            audioStreamUrl = "https://euronews-french-1-eu.plex.wurl.tv/playlist.m3u8",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Haber",
            logoAsset = "logos/brand/euronews.webp"
        ),
        NewsChannel(
            id = "fr_nrj",
            title = "NRJ",
            url = "https://www.nrj.fr/",
            audioStreamUrl = "https://scdn.nrjaudio.fm/adwz2/fr/30001/mp3_128.mp3",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Radyo",
            logoAsset = "logos/brand/nrj.webp"
        ),
        NewsChannel(
            id = "fr_skyrock",
            title = "Skyrock",
            url = "https://skyrock.fm/",
            audioStreamUrl = "https://icecast.skyrock.net/s/natio_mp3_128k",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Radyo",
            logoAsset = "logos/brand/skyrock.webp"
        ),
        NewsChannel(
            id = "fr_franceinter",
            title = "France Inter",
            url = "https://www.radiofrance.fr/franceinter",
            audioStreamUrl = "https://stream.radiofrance.fr/franceinter/franceinter.m3u8",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Radyo",
            logoAsset = "logos/brand/franceinter.webp"
        ),
        NewsChannel(
            id = "fr_rtl",
            title = "RTL France",
            url = "https://www.rtl.fr/",
            audioStreamUrl = "http://icecast.rtl.fr/rtl-1-44-128?listen=webCwsBCggNCQgLDQUGBAcGBg",
            countryCode = "FR",
            countryName = "Fransa",
            category = "Radyo",
            logoAsset = "logos/brand/rtlfr.webp"
        ),

        // ==========================================
        // 🇮🇹 İTALYA KANALLARI & RADYOLARI
        // ==========================================
        NewsChannel(
            id = "it_rainews24",
            title = "Rai News 24",
            url = "https://www.rainews.it/dirette",
            audioStreamUrl = "https://mediapolisvod.rai.it/relinker/relinkerServlet.htm?cont=1",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Haber",
            logoAsset = "logos/brand/rainews24.webp"
        ),
        NewsChannel(
            id = "it_tgcom24",
            title = "TGCOM24",
            url = "https://www.tgcom24.mediaset.it/live/",
            audioStreamUrl = "https://d3k8wzt41aflvx.cloudfront.net/TGCOM24/Live.m3u8",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Haber",
            logoAsset = "logos/brand/tgcom24.webp"
        ),
        NewsChannel(
            id = "it_skytg24",
            title = "Sky TG24",
            url = "https://tg24.sky.it/diretta",
            audioStreamUrl = null,
            countryCode = "IT",
            countryName = "İtalya",
            category = "Haber",
            logoAsset = "logos/brand/skytg24.webp"
        ),
        NewsChannel(
            id = "it_rtl1025tv",
            title = "RTL 102.5 TV",
            url = "https://play.rtl.it/",
            audioStreamUrl = "https://dd782ed59e2a4e86aabf6fc508674b59.msvdn.net/live/S76960628/OEPHRUIctA0M/playlist_video.m3u8",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Müzik",
            logoAsset = "logos/brand/rtl1025.webp"
        ),
        NewsChannel(
            id = "it_rai1",
            title = "Rai 1",
            url = "https://www.raiplay.it/dirette/rai1",
            audioStreamUrl = "https://mediapolis.rai.it/relinker/relinkerServlet.htm?cont=260682",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Ulusal",
            logoAsset = "logos/brand/rai1.webp"
        ),
        NewsChannel(
            id = "it_rtl1025",
            title = "RTL 102.5",
            url = "https://www.rtl.it/",
            audioStreamUrl = "https://streaming.rtl.it/rtv/mp3/128",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Radyo",
            logoAsset = "logos/brand/rtl1025.webp"
        ),
        NewsChannel(
            id = "it_radio105",
            title = "Radio 105",
            url = "https://www.105.net/",
            audioStreamUrl = "https://icecast.unitedradio.it/Radio105.mp3",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Radyo",
            logoAsset = "logos/brand/radio105.webp"
        ),
        NewsChannel(
            id = "it_rds",
            title = "RDS",
            url = "https://www.rds.it/",
            audioStreamUrl = "https://stream.rds.radio/audio/rds.stream_aac64/chunklist.m3u8",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Radyo",
            logoAsset = "logos/brand/rds.webp"
        ),
        NewsChannel(
            id = "it_radiodeejay",
            title = "Radio Deejay",
            url = "https://www.deejay.it/",
            audioStreamUrl = "https://4c4b867c89244861ac216426883d1ad0.msvdn.net/live/S85984808/sMO0tz9Sr2Rk/playlist.m3u8",
            countryCode = "IT",
            countryName = "İtalya",
            category = "Radyo",
            logoAsset = "logos/brand/radiodeejay.webp"
        ),

        // ==========================================
        // 🇪🇸 İSPANYA KANALLARI & RADYOLARI
        // ==========================================
        NewsChannel(
            id = "es_rtve24h",
            title = "RTVE 24h",
            url = "https://www.rtve.es/play/videos/directo/canales-lineales/24h/",
            audioStreamUrl = "https://ztnr.rtve.es/ztnr/1694255.m3u8",
            countryCode = "ES",
            countryName = "İspanya",
            category = "Haber",
            logoAsset = "logos/brand/rtve24h.webp"
        ),
        NewsChannel(
            id = "es_la1",
            title = "La 1",
            url = "https://www.rtve.es/play/videos/directo/canales-lineales/la-1/",
            audioStreamUrl = "https://rtvelivestream.rtve.es/rtvesec/la1/la1_main.m3u8",
            countryCode = "ES",
            countryName = "İspanya",
            category = "Ulusal",
            logoAsset = "logos/brand/la1.webp"
        ),
        NewsChannel(
            id = "es_antena3",
            title = "Antena 3",
            url = "https://www.atresplayer.com/directos/antena3/",
            audioStreamUrl = null,
            countryCode = "ES",
            countryName = "İspanya",
            category = "Ulusal",
            logoAsset = "logos/brand/antena3.webp"
        ),
        NewsChannel(
            id = "es_telecinco",
            title = "Telecinco",
            url = "https://www.telecinco.es/endirecto/",
            audioStreamUrl = null,
            countryCode = "ES",
            countryName = "İspanya",
            category = "Ulusal",
            logoAsset = "logos/brand/telecinco.webp"
        ),
        NewsChannel(
            id = "es_cuatro",
            title = "Cuatro",
            url = "https://www.cuatro.com/en-directo/",
            audioStreamUrl = null,
            countryCode = "ES",
            countryName = "İspanya",
            category = "Ulusal",
            logoAsset = "logos/brand/cuatro.webp"
        ),
        NewsChannel(
            id = "es_lasexta",
            title = "La Sexta",
            url = "https://www.atresplayer.com/directos/lasexta/",
            audioStreamUrl = null,
            countryCode = "ES",
            countryName = "İspanya",
            category = "Ulusal",
            logoAsset = "logos/brand/lasexta.webp"
        ),
        NewsChannel(
            id = "es_cadenaser",
            title = "Cadena SER",
            url = "https://cadenaser.com/",
            audioStreamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3",
            countryCode = "ES",
            countryName = "İspanya",
            category = "Radyo",
            logoAsset = "logos/brand/cadenaser.webp"
        ),
        NewsChannel(
            id = "es_los40",
            title = "Los 40",
            url = "https://los40.com/",
            audioStreamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40.mp3",
            countryCode = "ES",
            countryName = "İspanya",
            category = "Radyo",
            logoAsset = "logos/brand/los40.webp"
        ),
        NewsChannel(
            id = "es_cadenadial",
            title = "Cadena Dial",
            url = "https://www.cadenadial.com/",
            audioStreamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIAL.mp3",
            countryCode = "ES",
            countryName = "İspanya",
            category = "Radyo",
            logoAsset = "logos/brand/cadenadial.webp"
        ),
        NewsChannel(
            id = "es_ondacero",
            title = "Onda Cero",
            url = "https://www.ondacero.es/",
            audioStreamUrl = "https://atres-live.ondacero.es/live/ondacero/bitrate_1.m3u8",
            countryCode = "ES",
            countryName = "İspanya",
            category = "Radyo",
            logoAsset = "logos/brand/ondacero.webp"
        )
    )

    fun getSupportedCountries(): List<Pair<String, String>> = cache().supported

    fun getChannelsForCountry(countryCode: String, customList: List<NewsChannel> = emptyList()): List<NewsChannel> {
        val merged = if (customList.isEmpty()) cache().mergedDistinct else
            (defaultChannels + bundledChannels + customList)
                .distinctBy { it.countryCode + "|" + it.title.lowercase(Locale.ROOT) }
        val targetCode = if (countryCode.equals("UK", ignoreCase = true)) "GB" else countryCode
        val filtered = merged.filter {
            it.countryCode.equals(targetCode, ignoreCase = true) ||
            (targetCode.equals("GB", ignoreCase = true) && it.countryCode.equals("UK", ignoreCase = true))
        }
        return if (filtered.isNotEmpty()) filtered else merged.filter { it.countryCode == "GLOBAL" }
    }

    /** Kullanıcının seçtiği ülke; seçim yoksa telefonun bulunduğu ülke. */
    fun detectUserCountry(): String {
        val saved = appContext?.getSharedPreferences(COUNTRY_PREFS, android.content.Context.MODE_PRIVATE)
            ?.getString(KEY_SELECTED_COUNTRY, null)
        if (saved != null && getSupportedCountries().any { it.first == saved }) return saved
        return deviceCountry()
    }

    /** Telefonun ülkesi: SIM → mobil ağ → bölge ayarı. Desteklenmeyen ülkede Global liste. */
    fun deviceCountry(): String {
        val tm = appContext?.getSystemService(android.telephony.TelephonyManager::class.java)
        val supported = getSupportedCountries()
        return listOf(tm?.simCountryIso, tm?.networkCountryIso, Locale.getDefault().country)
            .map { it.orEmpty().uppercase(Locale.ROOT) }
            .firstOrNull { c -> c.isNotBlank() && supported.any { it.first == c } }
            ?: "GLOBAL"
    }

    fun saveSelectedCountry(code: String) {
        appContext?.getSharedPreferences(COUNTRY_PREFS, android.content.Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_SELECTED_COUNTRY, code)?.apply()
    }

    fun findById(id: String, customList: List<NewsChannel> = emptyList()): NewsChannel? {
        if (customList.isEmpty()) return cache().byId[id]
        return (defaultChannels + bundledChannels + customList).firstOrNull { it.id == id }
    }

    fun searchChannels(query: String, customList: List<NewsChannel> = emptyList()): List<NewsChannel> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase(Locale.ROOT)
        if (customList.isEmpty()) {
            return cache().searchable.filter { (_, l) ->
                l.first.contains(q) || l.second.contains(q) || l.third.contains(q)
            }.map { it.first }
        }
        return (defaultChannels + bundledChannels + customList)
            .distinctBy { it.id }
            .filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                it.category.lowercase(Locale.ROOT).contains(q) ||
                it.countryName.lowercase(Locale.ROOT).contains(q)
            }
    }

    fun getDeviceCountryCode(): String = detectUserCountry()
    val availableCountries: List<Pair<String, String>> get() = getSupportedCountries()

    fun getCarSupportedCountries() = getSupportedCountries()
    fun getCarAudioChannelsForCountry(countryCode: String, customList: List<NewsChannel> = emptyList()) = getChannelsForCountry(countryCode, customList)
    fun searchCarAudioChannels(query: String, customList: List<NewsChannel> = emptyList()) = searchChannels(query, customList)
}
