package com.autolounge.park.data

data class CatalogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val category: String,
    val iconRes: Int? = null,
    val badge: String? = null,
    val isAppMode: Boolean = true
)

object AppCatalogRepository {

    val categories = listOf(
        "Tümü",
        "Video & Sinema",
        "Canlı TV",
        "Müzik & Radyo",
        "Yapay Zeka",
        "Haber & Finans"
    )

    val items: List<CatalogItem> = listOf(
        // ==========================================
        // 🎬 VİDEO & SİNEMA & DİZİ
        // ==========================================
        CatalogItem(
            id = "cat_youtube",
            title = "YouTube",
            subtitle = "Sınırsız Video & Müzik",
            url = "https://m.youtube.com",
            category = "Video & Sinema",
            badge = "Popüler"
        ),
        CatalogItem(
            id = "cat_netflix",
            title = "Netflix",
            subtitle = "Dizi, Film ve Belgesel",
            url = "https://www.netflix.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_disney",
            title = "Disney+",
            subtitle = "Marvel, Star Wars, Pixar",
            url = "https://www.disneyplus.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_prime",
            title = "Prime Video",
            subtitle = "Amazon Orijinal Yapımları",
            url = "https://www.primevideo.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_twitch",
            title = "Twitch",
            subtitle = "Canlı Oyun & Yayınlar",
            url = "https://m.twitch.tv",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_kick",
            title = "Kick",
            subtitle = "Canlı Yayın Platformu",
            url = "https://kick.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_exxen",
            title = "Exxen",
            subtitle = "Diziler & Spor Yayınları",
            url = "https://www.exxen.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_gain",
            title = "GAİN",
            subtitle = "Yeni Nesil İçerikler",
            url = "https://www.gain.tv",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_blutv",
            title = "BluTV",
            subtitle = "Özel Diziler & Eğlence",
            url = "https://www.blutv.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_tabii",
            title = "TRT Tabii",
            subtitle = "TRT Dizileri, Filmler & Canlı TV",
            url = "https://www.tabii.com",
            category = "Video & Sinema",
            badge = "Popüler"
        ),
        CatalogItem(
            id = "cat_max",
            title = "Max",
            subtitle = "HBO, Warner Bros & DC",
            url = "https://www.max.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_appletv",
            title = "Apple TV+",
            subtitle = "Apple Orijinal Yapımları",
            url = "https://tv.apple.com",
            category = "Video & Sinema"
        ),
        CatalogItem(
            id = "cat_dailymotion",
            title = "Dailymotion",
            subtitle = "Video ve Haberler",
            url = "https://www.dailymotion.com",
            category = "Video & Sinema"
        ),

        // ==========================================
        // 📺 CANLI TV KANALLARI (TEK TIKLA KOKPİTE)
        // ==========================================
        CatalogItem(
            id = "cat_tv_trt1",
            title = "TRT 1",
            subtitle = "Canlı Ulusal Yayın",
            url = "https://www.trt1.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_atv",
            title = "ATV",
            subtitle = "Diziler & Canlı Yayın",
            url = "https://www.atv.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_showtv",
            title = "Show TV",
            subtitle = "Eğlence ve Diziler",
            url = "https://www.showtv.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_kanald",
            title = "Kanal D",
            subtitle = "Canlı Televizyon",
            url = "https://www.kanald.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_startv",
            title = "Star TV",
            subtitle = "Canlı Yayın & Programlar",
            url = "https://www.startv.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_now",
            title = "NOW (FOX)",
            subtitle = "Ulusal Televizyon",
            url = "https://www.nowtv.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_tv8",
            title = "TV8",
            subtitle = "Yarışma ve Eğlence",
            url = "https://www.tv8.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Canlı"
        ),
        CatalogItem(
            id = "cat_tv_trthaber",
            title = "TRT Haber",
            subtitle = "7/24 Kesintisiz Haber",
            url = "https://www.trthaber.com/canli-yayin-izle.html",
            category = "Canlı TV",
            badge = "Haber"
        ),
        CatalogItem(
            id = "cat_tv_ntv",
            title = "NTV",
            subtitle = "Son Dakika Haberleri",
            url = "https://www.ntv.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Haber"
        ),
        CatalogItem(
            id = "cat_tv_haberturk",
            title = "Habertürk TV",
            subtitle = "Gündem ve Tartışma",
            url = "https://www.haberturk.com/canli-yayin",
            category = "Canlı TV",
            badge = "Haber"
        ),
        CatalogItem(
            id = "cat_tv_sozcu",
            title = "Sözcü TV",
            subtitle = "Bağımsız Haber Kanalı",
            url = "https://www.sozcutv.com.tr/canli-yayin",
            category = "Canlı TV",
            badge = "Haber"
        ),
        CatalogItem(
            id = "cat_tv_trtspor",
            title = "TRT Spor",
            subtitle = "Canlı Maç & Spor Gündemi",
            url = "https://www.trtspor.com.tr/canli-yayin-izle/trt-spor",
            category = "Canlı TV",
            badge = "Spor"
        ),

        // ==========================================
        // 🎵 MÜZİK & RADYO
        // ==========================================
        CatalogItem(
            id = "cat_spotify",
            title = "Spotify Web",
            subtitle = "Milyonlarca Şarkı & Çalma Listesi",
            url = "https://open.spotify.com",
            category = "Müzik & Radyo",
            badge = "Popüler"
        ),
        CatalogItem(
            id = "cat_ytmusic",
            title = "YouTube Music",
            subtitle = "Resmi Müzik Akışı",
            url = "https://music.youtube.com",
            category = "Müzik & Radyo"
        ),
        CatalogItem(
            id = "cat_soundcloud",
            title = "SoundCloud",
            subtitle = "Bağımsız Parçalar & Remixler",
            url = "https://m.soundcloud.com",
            category = "Müzik & Radyo"
        ),
        CatalogItem(
            id = "cat_kralfm",
            title = "Kral FM",
            subtitle = "İlaç Gibi Radyo",
            url = "https://www.kralmuzik.com.tr/radyolar/kral-fm",
            category = "Müzik & Radyo",
            badge = "Radyo"
        ),
        CatalogItem(
            id = "cat_powerfm",
            title = "Power FM",
            subtitle = "En İyi Yabancı Müzik",
            url = "https://www.powerapp.com.tr/powerfm/",
            category = "Müzik & Radyo",
            badge = "Radyo"
        ),

        // ==========================================
        // 🤖 YAPAY ZEKA & ASİSTAN
        // ==========================================
        CatalogItem(
            id = "cat_chatgpt",
            title = "ChatGPT",
            subtitle = "OpenAI Akıllı Asistan",
            url = "https://chatgpt.com",
            category = "Yapay Zeka",
            badge = "AI"
        ),
        CatalogItem(
            id = "cat_gemini",
            title = "Gemini AI",
            subtitle = "Google Yeni Nesil Zeka",
            url = "https://gemini.google.com",
            category = "Yapay Zeka",
            badge = "AI"
        ),
        CatalogItem(
            id = "cat_claude",
            title = "Claude AI",
            subtitle = "Anthropic Zeka Motoru",
            url = "https://claude.ai",
            category = "Yapay Zeka",
            badge = "AI"
        ),

        // ==========================================
        // 📊 HABER & FİNANS & BİLGİ
        // ==========================================
        CatalogItem(
            id = "cat_tradingview",
            title = "TradingView",
            subtitle = "Canlı Borsa, Kripto & Grafikler",
            url = "https://tr.tradingview.com",
            category = "Haber & Finans",
            badge = "Borsa"
        ),
        CatalogItem(
            id = "cat_bloomberg",
            title = "Bloomberg HT",
            subtitle = "Piyasalar & Ekonomi",
            url = "https://www.bloomberght.com",
            category = "Haber & Finans"
        ),
        CatalogItem(
            id = "cat_maps",
            title = "Google Haritalar",
            subtitle = "Trafik & Canlı Konum",
            url = "https://maps.google.com",
            category = "Haber & Finans"
        ),
        CatalogItem(
            id = "cat_weather",
            title = "Hava Durumu",
            subtitle = "Meteoroloji & Tahmin",
            url = "https://weather.com/tr-TR",
            category = "Haber & Finans"
        ),
        CatalogItem(
            id = "cat_eksisozluk",
            title = "Ekşi Sözlük",
            subtitle = "Gündem & Başlıklar",
            url = "https://eksisozluk.com",
            category = "Haber & Finans"
        )
    )
}
