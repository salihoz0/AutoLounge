package com.autohub.park.data

object DefaultBookmarks {
    private val common = listOf(
        Bookmark(title = "YouTube", url = "https://m.youtube.com", type = "site", isDefault = true),
        Bookmark(title = "Google", url = "https://www.google.com", type = "site", isDefault = true),
    )

    // Ülkeye özel yayın kısayolları; listede olmayan ülkelere ücretsiz global TV
    private val byCountry = mapOf(
        "TR" to listOf(
            Bookmark(title = "TRT Tabii", url = "https://www.tabii.com", type = "site", isDefault = true),
            Bookmark(title = "TRT Haber", url = "https://www.trthaber.com/canli-yayin-izle.html", type = "tv", isDefault = true),
        ),
        "US" to listOf(
            Bookmark(title = "Pluto TV", url = "https://pluto.tv", type = "site", isDefault = true),
            Bookmark(title = "ABC News Live", url = "https://abcnews.go.com/live", type = "tv", isDefault = true),
        ),
        "GB" to listOf(
            Bookmark(title = "BBC iPlayer", url = "https://www.bbc.co.uk/iplayer", type = "site", isDefault = true),
            Bookmark(title = "ITVX", url = "https://www.itv.com", type = "site", isDefault = true),
        ),
        "DE" to listOf(
            Bookmark(title = "ARD Mediathek", url = "https://www.ardmediathek.de", type = "site", isDefault = true),
            Bookmark(title = "ZDF Live", url = "https://www.zdf.de/live-tv", type = "tv", isDefault = true),
        ),
        "FR" to listOf(
            Bookmark(title = "france.tv", url = "https://www.france.tv", type = "site", isDefault = true),
        ),
        "IT" to listOf(
            Bookmark(title = "RaiPlay", url = "https://www.raiplay.it", type = "site", isDefault = true),
        ),
        "ES" to listOf(
            Bookmark(title = "RTVE Play", url = "https://www.rtve.es/play/", type = "site", isDefault = true),
        ),
    )

    private val global = listOf(
        Bookmark(title = "Pluto TV", url = "https://pluto.tv", type = "site", isDefault = true),
    )

    fun forCountry(code: String): List<Bookmark> = common + (byCountry[code] ?: global)
}
