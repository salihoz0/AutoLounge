package com.autohub.park.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val type: String = "site", // site | tv | radio
    val iconUrl: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(url.startsWith("https://")) { "Sadece https URL eklenebilir" }
        require(title.length <= 24) { "İsim en fazla 24 karakter" }
    }
}
