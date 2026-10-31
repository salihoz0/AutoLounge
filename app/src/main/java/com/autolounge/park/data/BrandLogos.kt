package com.autohub.park.data

import java.util.Locale

object BrandLogos {

    private val slugs = mapOf(
        "cat_youtube" to "youtube",
        "cat_netflix" to "netflix",
        "cat_disney" to "disneyplus",
        "cat_prime" to "primevideo",
        "cat_twitch" to "twitch",
        "cat_kick" to "kick",
        "cat_exxen" to "exxen",
        "cat_gain" to "gain",
        "cat_blutv" to "blutv",
        "cat_tabii" to "tabii",
        "cat_max" to "max",
        "cat_appletv" to "appletv",
        "cat_dailymotion" to "dailymotion",
        "cat_tv_trt1" to "trt1",
        "cat_tv_atv" to "atv",
        "cat_tv_showtv" to "showtv",
        "cat_tv_kanald" to "kanald",
        "cat_tv_startv" to "startv",
        "cat_tv_now" to "now",
        "cat_tv_tv8" to "tv8",
        "cat_tv_kanal7" to "kanal7",
        "cat_tv_beyaztv" to "beyaztv",
        "cat_tv_teve2" to "teve2",
        "cat_tv_tlc" to "tlc",
        "cat_tv_dmax" to "dmax",
        "cat_tv_trthaber" to "trthaber",
        "cat_tv_ntv" to "ntv",
        "cat_tv_haberturk" to "haberturk",
        "cat_tv_cnnturk" to "cnnturk",
        "cat_tv_sozcu" to "sozcu",
        "cat_tv_halktv" to "halktv",
        "cat_tv_haberglobal" to "haberglobal",
        "cat_tv_tgrthaber" to "tgrthaber",
        "cat_tv_tv100" to "tv100",
        "cat_tv_tele1" to "tele1",
        "cat_tv_ulketv" to "ulketv",
        "cat_tv_ekoturk" to "ekoturk",
        "cat_tv_24tv" to "24tv",
        "cat_tv_tv360" to "tv360",
        "cat_tv_tv4" to "tv4",
        "cat_tv_cnbce" to "cnbce",
        "cat_tv_flashhaber" to "flashhaber",
        "cat_tv_tbmm" to "tbmm",
        "cat_tv_trtspor" to "trtspor",
        "cat_tv_trtsporyildiz" to "trtsporyildiz",
        "cat_tv_aspor" to "aspor",
        "cat_tv_fbtv" to "fbtv",
        "cat_tv_sportstv" to "sportstv",
        "cat_tv_trtbelgesel" to "trtbelgesel",
        "cat_tv_trtcocuk" to "trtcocuk",
        "cat_tv_minikacocuk" to "minikacocuk",
        "cat_tv_minikago" to "minikago",
        "cat_tv_kralpop" to "kralpop",
        "cat_tv_kraltv" to "kraltv",
        "cat_tv_powerturk" to "powerturk",
        "cat_tv_powertv" to "powertv",
        "cat_tv_dreamturk" to "dreamturk",
        "cat_tv_trtmuzik" to "trtmuzik",
        "cat_tv_numberone" to "numberone",
        "cat_tv_trt2" to "trt2",
        "cat_tv_trtavaz" to "trtavaz",
        "cat_tv_trtturk" to "trtturk",
        "cat_spotify" to "spotify",
        "cat_ytmusic" to "ytmusic",
        "cat_soundcloud" to "soundcloud",
        "cat_kralfm" to "kralfm",
        "cat_powerfm" to "powerfm",
        "cat_powerturkfm" to "powerturk",
        "cat_slowturk" to "slowturk",
        "cat_superfm" to "superfm",
        "cat_joyturk" to "joyturk",
        "cat_trtfm" to "trtfm",
        "cat_trtturku" to "trtturku",
        "cat_trtradyo1" to "trtradyo1",
        "cat_chatgpt" to "chatgpt",
        "cat_gemini" to "gemini",
        "cat_claude" to "claude",
        "cat_tradingview" to "tradingview",
        "cat_bloomberg" to "bloomberght",
        "cat_maps" to "googlemaps",
        "cat_weather" to "weather",
        "cat_eksisozluk" to "eksisozluk"
    )

    fun uriFor(catalogId: String): String? = slugs[catalogId]?.let { uri(it) }

    fun uri(slug: String): String = "file:///android_asset/logos/brand/$slug.webp"

    fun findLogoByUrlOrTitle(title: String, url: String): String? {
        val t = title.trim().lowercase(Locale.ROOT)
        val u = url.trim().lowercase(Locale.ROOT)

        return when {
            // Dijital Platformlar & Servisler
            u.contains("tabii.com") || t.contains("tabii") -> uri("tabii")
            u.contains("youtube.com") || u.contains("youtu.be") || t.contains("youtube") -> uri("youtube")
            u.contains("netflix.com") || t.contains("netflix") -> uri("netflix")
            u.contains("disneyplus.com") || t.contains("disney") -> uri("disneyplus")
            u.contains("primevideo.com") || t.contains("prime") -> uri("primevideo")
            u.contains("twitch.tv") || t.contains("twitch") -> uri("twitch")
            u.contains("kick.com") || t.contains("kick") -> uri("kick")
            u.contains("exxen.com") || t.contains("exxen") -> uri("exxen")
            u.contains("gain.tv") || t.contains("gain") || title.contains("GAİN") -> uri("gain")
            u.contains("blutv.com") || t.contains("blutv") -> uri("blutv")
            (!t.contains("dmax") && !u.contains("dmax")) && (u.contains("max.com") || t == "max" || t.contains("hbo")) -> uri("max")
            u.contains("apple.com") || t.contains("apple tv") -> uri("appletv")
            u.contains("dailymotion.com") || t.contains("dailymotion") -> uri("dailymotion")
            u.contains("spotify.com") || t.contains("spotify") -> uri("spotify")
            u.contains("music.youtube.com") || t.contains("yt music") -> uri("ytmusic")
            u.contains("soundcloud.com") || t.contains("soundcloud") -> uri("soundcloud")
            u.contains("chatgpt.com") || t.contains("chatgpt") -> uri("chatgpt")
            u.contains("gemini.google.com") || t.contains("gemini") -> uri("gemini")
            u.contains("claude.ai") || t.contains("claude") -> uri("claude")
            u.contains("tradingview.com") || t.contains("tradingview") -> uri("tradingview")
            u.contains("maps.google.com") || t.contains("haritalar") -> uri("googlemaps")
            u.contains("weather.com") || t.contains("hava durumu") -> uri("weather")
            u.contains("eksisozluk.com") || t.contains("ekşi sözlük") || t.contains("eksi sozluk") -> uri("eksisozluk")

            // Ulusal Kanallar (TR)
            u.contains("kanald.com") || t.contains("kanal d") || t == "kanald" -> uri("kanald")
            u.contains("kanal7.com") || t.contains("kanal 7") || t == "kanal7" -> uri("kanal7")
            u.contains("beyaztv.com") || t.contains("beyaz tv") || t == "beyaztv" -> uri("beyaztv")
            u.contains("teve2.com") || t.contains("teve2") || t.contains("teve 2") -> uri("teve2")
            u.contains("tlctv.com") || t.contains("tlc") -> uri("tlc")
            u.contains("dmaxtv.com") || t.contains("dmax") -> uri("dmax")
            u.contains("showtv.com") || t.contains("show tv") || t.contains("showtv") -> uri("showtv")
            u.contains("startv.com") || t.contains("star tv") || t.contains("startv") -> uri("startv")
            u.contains("atv.com") || t == "atv" || t.startsWith("atv") -> uri("atv")
            u.contains("tv8") || t.contains("tv8") || t.contains("tv 8") -> uri("tv8")
            u.contains("nowtv.com") || t == "now" || t == "now tv" || t.contains("now (fox)") -> uri("now")
            u.contains("tv4.com") || t == "tv4" || t == "tv 4" -> uri("tv4")
            u.contains("eurod.com") || t.contains("euro d") -> uri("eurod")

            // Haber & Ekonomi (TR)
            u.contains("trthaber.com") || t.contains("trt haber") -> uri("trthaber")
            u.contains("trt1.com") || t == "trt 1" -> uri("trt1")
            u.contains("haberturk.com") || t.contains("habertürk") || t.contains("haberturk") -> uri("haberturk")
            u.contains("ntv.com") || t == "ntv" || t.startsWith("ntv") -> uri("ntv")
            u.contains("cnnturk.com") || t.contains("cnn türk") || t.contains("cnn turk") -> uri("cnnturk")
            u.contains("sozcutv.com") || t.contains("sözcü") || t.contains("sozcu") -> uri("sozcu")
            u.contains("halktv.com") || t.contains("halk tv") || t.contains("halktv") -> uri("halktv")
            u.contains("haberglobal.com") || t.contains("haber global") -> uri("haberglobal")
            u.contains("bloomberght.com") || t.contains("bloomberg ht") -> uri("bloomberght")
            u.contains("ahaber.com") || t.contains("a haber") -> uri("ahaber")
            u.contains("tgrthaber.com") || t.contains("tgrt") -> uri("tgrthaber")
            u.contains("tv100.com") || t.contains("tv100") || t.contains("tv 100") -> uri("tv100")
            u.contains("tele1.com") || t.contains("tele1") || t.contains("tele 1") -> uri("tele1")
            u.contains("ulketv.com") || t.contains("ülke tv") || t.contains("ulke tv") -> uri("ulketv")
            u.contains("ekoturk.com") || t.contains("ekotürk") || t.contains("ekoturk") -> uri("ekoturk")
            u.contains("tv24.com") || t.contains("24 tv") || t.contains("yirmidort") -> uri("24tv")
            u.contains("tv360.com") || t.contains("360") -> uri("tv360")
            u.contains("cnbce.com") || t.contains("cnbc-e") || t.contains("cnbce") -> uri("cnbce")
            u.contains("flashhaber") || t.contains("flash haber") || t.contains("flash tv") -> uri("flashhaber")
            u.contains("tbmm.gov") || t.contains("tbmm") || t.contains("meclis tv") -> uri("tbmm")
            u.contains("benguturk") || t.contains("bengü türk") || t.contains("bengu turk") -> uri("benguturk")
            u.contains("krttv") || t.contains("krt") -> uri("krttv")

            // Spor (TR)
            u.contains("trtspor.com.tr/canli-yayin-izle/trt-spor-yildiz") || t.contains("yıldız") || t.contains("yildiz") -> uri("trtsporyildiz")
            u.contains("trtspor.com") || t.contains("trt spor") -> uri("trtspor")
            u.contains("aspor.com") || t.contains("a spor") || t == "aspor" -> uri("aspor")
            u.contains("fenerbahce.org") || t.contains("fenerbahçe") || t.contains("fenerbahce") || t == "fbtv" -> uri("fbtv")
            u.contains("sportstv.com") || t.contains("sports tv") || t == "sportstv" -> uri("sportstv")
            u.contains("htspor") || t.contains("ht spor") -> uri("htspor")
            u.contains("beinsports") || t.contains("bein") -> uri("beinsports")

            // Belgesel & Çocuk & Kültür & Müzik (TR)
            u.contains("trtbelgesel.com") || t.contains("trt belgesel") -> uri("trtbelgesel")
            u.contains("trtcocuk.net") || t.contains("trt çocuk") || t.contains("trt cocuk") -> uri("trtcocuk")
            u.contains("minikacocuk.com") || t.contains("minika çocuk") || t.contains("minika cocuk") -> uri("minikacocuk")
            u.contains("minikago.com") || t.contains("minika go") -> uri("minikago")
            u.contains("kralpoptv") || t.contains("kral pop") -> uri("kralpop")
            u.contains("kralmuzik.com.tr/tv/kral-tv") || t == "kral tv" -> uri("kraltv")
            u.contains("kralmuzik.com") || t.contains("kral") -> uri("kralfm")
            (u.contains("powerapp.com") || t.contains("power")) && (t.contains("türk") || t.contains("turk")) -> uri("powerturk")
            (u.contains("powerapp.com") || t.contains("power")) && t.contains("pop") -> uri("powerpop")
            u.contains("powerapp.com/power-tv") || t.contains("power tv") -> uri("powertv")
            u.contains("powerapp.com") || t.contains("power fm") -> uri("powerfm")
            u.contains("dreamturk.com") || t.contains("dream türk") || t.contains("dream turk") -> uri("dreamturk")
            u.contains("trtmuzik") || t.contains("trt müzik") || t.contains("trt muzik") -> uri("trtmuzik")
            u.contains("numberone.com.tr/n1turktv") || t.contains("number 1 türk") || t.contains("nr1 türk") -> uri("numberoneturk")
            u.contains("numberone.com") || t.contains("number 1") || t.contains("nr1") -> uri("numberone")
            u.contains("trt2.com") || t == "trt 2" -> uri("trt2")
            u.contains("trtavaz.com") || t.contains("trt avaz") -> uri("trtavaz")
            u.contains("trtturk.com") || t.contains("trt türk") || t.contains("trt turk") -> uri("trtturk")

            // Radyolar (TR)
            u.contains("slowturk.com") || t.contains("slow türk") || t.contains("slow turk") -> uri("slowturk")
            u.contains("superfm") || t.contains("süper fm") || t.contains("super fm") -> uri("superfm")
            u.contains("joyturk") || t.contains("joy türk") || t.contains("joy turk") -> uri("joyturk")
            u.contains("trt-fm") || t.contains("trt fm") -> uri("trtfm")
            u.contains("trt-turku") || t.contains("trt türkü") || t.contains("trt turku") -> uri("trtturku")
            u.contains("trt-radyo-1") || t.contains("trt radyo 1") || t.contains("trt radyo") -> uri("trtradyo1")
            u.contains("alemfm") || t.contains("alem fm") -> uri("alemfm")
            u.contains("bestfm") || t.contains("best fm") -> uri("bestfm")
            u.contains("showradyo") || t.contains("show radyo") -> uri("showradyo")
            u.contains("radyod") || t.contains("radyo d") -> uri("radyod")
            u.contains("virginradio") || t.contains("virgin") -> uri("virginradio")

            // Global
            u.contains("bbc.com") || t.contains("bbc news") || t.contains("bbc world") -> uri("bbcnews")
            u.contains("edition.cnn.com") || t.contains("cnn international") || t == "cnn" -> uri("cnn")
            u.contains("aljazeera.com") || t.contains("al jazeera") -> uri("aljazeera")
            u.contains("euronews.com") || t.contains("euronews") -> uri("euronews")
            u.contains("dw.com") || t.contains("deutsche welle") || t == "dw" || t.startsWith("dw ") -> uri("dw")
            u.contains("france24.com") || t.contains("france 24") || t.contains("france24") -> uri("france24")
            u.contains("bloomberg.com/tv") || t.contains("bloomberg tv") || t.contains("bloomberg television") -> uri("bloombergtv")
            u.contains("bloomberg.com/audio") || t.contains("bloomberg radio") -> uri("bloombergradio")
            u.contains("bloomberg.com") || t.contains("bloomberg") -> uri("bloomberg")
            u.contains("trtworld.com") || t.contains("trt world") -> uri("trtworld")
            u.contains("cgtn.com") || t.contains("cgtn") -> uri("cgtn")
            u.contains("nhk.or.jp") || t.contains("nhk") -> uri("nhkworld")
            u.contains("channelnewsasia.com") || t.contains("cna") || t.contains("channel newsasia") -> uri("cna")
            u.contains("arirang.com") || t.contains("arirang") -> uri("arirang")
            u.contains("wionews.com") || t.contains("wion") -> uri("wion")

            // Azerbaycan (AZ)
            u.contains("itv.az") || t.contains("ictimai") || t.contains("i̇ctimai") || t == "itv" -> uri("itv")
            u.contains("aztv.az") || t.contains("aztv") -> uri("aztv")
            u.contains("cbcsport.az") || t.contains("cbc sport") -> uri("cbcsport")
            u.contains("xezertv.az") || t.contains("xezer") || t.contains("xəzər") -> uri("xezertv")
            u.contains("spacetv.az") || t.contains("space tv") -> uri("spacetv")
            u.contains("arbtv.az") || t.contains("arb") -> uri("arbtv")
            u.contains("baku.tv") || t.contains("baku tv") -> uri("bakutv")
            u.contains("realtv.az") || t.contains("real tv") -> uri("realtv")
            u.contains("idmantv.az") || t.contains("idman") || t.contains("i̇dman") -> uri("idmantv")
            u.contains("medeniyyet") || t.contains("mədəniyyət") -> uri("medeniyyettv")

            // Almanya (DE)
            u.contains("tagesschau24") || t.contains("tagesschau24") -> uri("tagesschau24")
            u.contains("tagesschau.de") || t.contains("tagesschau") -> uri("tagesschau24")
            u.contains("zdfinfo") || t.contains("zdfinfo") || t.contains("zdf info") -> uri("zdfinfo")
            u.contains("zdf.de") || t.contains("zdf") -> uri("zdf")
            u.contains("n-tv.de") || t.contains("n-tv") -> uri("ntvde")
            u.contains("welt.de") || t.contains("welt") -> uri("welt")
            u.contains("daserste.de") || t.contains("das erste") || t.contains("ard") -> uri("daserste")
            u.contains("rtl.de") || t == "rtl" || t.contains("rtl television") -> uri("rtl")
            u.contains("prosieben.de") || t.contains("prosieben") || t.contains("pro7") -> uri("prosieben")
            u.contains("sat1.de") || t.contains("sat.1") || t.contains("sat1") -> uri("sat1")
            u.contains("vox.de") || t.contains("vox") -> uri("vox")
            u.contains("kika.de") || t.contains("kika") -> uri("kika")
            u.contains("3sat.de") || t.contains("3sat") || t.contains("dreisat") -> uri("dreisat")
            u.contains("arte.tv") || t.contains("arte") -> uri("arte")
            u.contains("phoenix.de") || t.contains("phoenix") -> uri("phoenix")
            u.contains("antenne.de") || t.contains("antenne bayern") -> uri("antennebayern")
            u.contains("1live") || t.contains("1live") || t.contains("einslive") -> uri("einslive")
            u.contains("deutschlandfunk") || t.contains("dlf") || t.contains("deutschlandfunk") -> uri("dlf")
            u.contains("swr3") || t.contains("swr3") -> uri("swr3")

            // Birleşik Krallık (GB / UK)
            u.contains("news.sky.com") || t.contains("sky news") -> uri("skynews")
            u.contains("bbc.co.uk/bbcone") || t.contains("bbc one") -> uri("bbcone")
            u.contains("bbc.co.uk/bbctwo") || t.contains("bbc two") -> uri("bbctwo")
            u.contains("itv.com/hub/itv2") || t.contains("itv2") || t.contains("itv 2") -> uri("itv2")
            u.contains("itv.com") || t.contains("itv1") || t.contains("itv 1") || t == "itv" -> uri("itv1")
            u.contains("channel4.com") || t.contains("channel 4") || t.contains("channel4") -> uri("channel4")
            u.contains("channel5.com") || t.contains("channel 5") || t.contains("channel5") -> uri("channel5")
            u.contains("gbnews.com") || t.contains("gb news") || t.contains("gbnews") -> uri("gbnews")
            u.contains("talk.tv") || t.contains("talktv") || t.contains("talk tv") -> uri("talktv")
            u.contains("skysports") || t.contains("sky sports news") || t.contains("sky sports") -> uri("skysportsnews")
            u.contains("capitalfm") || t.contains("capital fm") || t.contains("capital uk") -> uri("capitalfm")
            u.contains("heart.co.uk") || t.contains("heart uk") || t.contains("heart fm") -> uri("heartuk")
            u.contains("bbc.co.uk/radio1") || t.contains("bbc radio 1") || t.contains("bbc radio1") -> uri("bbcradio1")
            u.contains("bbc.co.uk/radio2") || t.contains("bbc radio 2") || t.contains("bbc radio2") -> uri("bbcradio2")
            u.contains("lbc.co.uk") || t.contains("lbc") -> uri("lbc")
            u.contains("classicfm") || t.contains("classic fm") -> uri("classicfm")

            // Amerika Birleşik Devletleri (US)
            u.contains("abcnews.go.com") || t.contains("abc news") -> uri("abcnews")
            u.contains("cbsnews.com") || t.contains("cbs news") -> uri("cbsnews")
            u.contains("nbcnews.com") || t.contains("nbc news") -> uri("nbcnews")
            u.contains("livenowfox") || t.contains("livenow") || t.contains("live now from fox") -> uri("livenowfox")
            u.contains("scrippsnews") || t.contains("scripps news") -> uri("scrippsnews")
            u.contains("cheddar.com") || t.contains("cheddar") -> uri("cheddarnews")
            u.contains("nasa.gov") || t.contains("nasa tv") || t == "nasa" -> uri("nasatv")
            u.contains("weathernation") || t.contains("weather nation") -> uri("weathernation")
            u.contains("foxnews.com") || t.contains("fox news") -> uri("foxnews")
            u.contains("msnbc.com") || t.contains("msnbc") -> uri("msnbc")
            u.contains("pbs.org") || t.contains("pbs") -> uri("pbs")
            u.contains("cnbc.com") || t.contains("cnbc") -> uri("cnbc")
            u.contains("c-span.org") || t.contains("c-span") || t.contains("cspan") -> uri("cspan")
            u.contains("npr.org") || t.contains("npr") -> uri("npr")
            u.contains("foxsportsradio") || t.contains("fox sports radio") -> uri("foxsportsradio")
            u.contains("espn.com/radio") || t.contains("espn radio") -> uri("espnradio")
            u.contains("weather.com") || t.contains("weather channel") -> uri("weatherchannel")

            // Fransa (FR)
            u.contains("bfmtv.com") || t.contains("bfm tv") || t.contains("bfmtv") -> uri("bfmtv")
            u.contains("cnews.fr") || t.contains("cnews") -> uri("cnews")
            u.contains("tf1info.fr/lci") || t.contains("lci") -> uri("lci")
            u.contains("francetvinfo.fr") || t.contains("franceinfo") || t.contains("france info") -> uri("franceinfo")
            u.contains("tv5monde.com") || t.contains("tv5monde") || t.contains("tv5 monde") -> uri("tv5monde")
            u.contains("arte.tv/fr") || t.contains("arte fr") || t.contains("arte français") -> uri("artefr")
            u.contains("nrj.fr") || t.contains("nrj") -> uri("nrj")
            u.contains("skyrock.com") || t.contains("skyrock") -> uri("skyrock")
            u.contains("radiofrance.fr/franceinter") || t.contains("france inter") -> uri("franceinter")
            u.contains("rtl.fr") || t.contains("rtl fr") || t.contains("rtl france") -> uri("rtlfr")

            // İtalya (IT)
            u.contains("rainews.it") || t.contains("rai news 24") || t.contains("rainews24") -> uri("rainews24")
            u.contains("tgcom24.mediaset.it") || t.contains("tgcom24") || t.contains("tgcom 24") -> uri("tgcom24")
            u.contains("tg24.sky.it") || t.contains("sky tg24") || t.contains("skytg24") -> uri("skytg24")
            u.contains("rtl.it") || t.contains("rtl 102.5") || t.contains("rtl 1025") -> uri("rtl1025")
            u.contains("raiplay.it/dirette/rai1") || t.contains("rai 1") || t == "rai1" -> uri("rai1")
            u.contains("105.net") || t.contains("radio 105") || t.contains("radio105") -> uri("radio105")
            u.contains("rds.it") || t.contains("rds") -> uri("rds")
            u.contains("deejay.it") || t.contains("radio deejay") || t.contains("radiodeejay") -> uri("radiodeejay")

            // İspanya (ES)
            u.contains("rtve.es/play/videos/canal-24-horas") || t.contains("rtve 24h") || t.contains("24 horas") -> uri("rtve24h")
            u.contains("rtve.es/play/videos/la-1") || t.contains("la 1") || t == "la1" -> uri("la1")
            u.contains("atresplayer.com/antena3") || t.contains("antena 3") || t.contains("antena3") -> uri("antena3")
            u.contains("telecinco.es") || t.contains("telecinco") -> uri("telecinco")
            u.contains("cuatro.com") || t.contains("cuatro") -> uri("cuatro")
            u.contains("lasexta.com") || t.contains("lasexta") || t.contains("la sexta") -> uri("lasexta")
            u.contains("cadenaser.com") || t.contains("cadena ser") -> uri("cadenaser")
            u.contains("los40.com") || t.contains("los 40") || t.contains("los40") -> uri("los40")
            u.contains("cadenadial.com") || t.contains("cadena dial") -> uri("cadenadial")
            u.contains("ondacero.es") || t.contains("onda cero") -> uri("ondacero")

            else -> null
        }
    }
}
