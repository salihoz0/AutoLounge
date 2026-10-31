package com.autohub.park.ui.components

import androidx.compose.runtime.Composable
import com.autohub.park.data.Bookmark

@Composable
fun NewsChannelsSheet(
    bookmarks: List<Bookmark>,
    onOpenUrl: (String) -> Unit,
    onPinChannel: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    LiveTvChannelsSheet(
        bookmarks = bookmarks,
        onSelectChannel = { ch -> onOpenUrl(ch.url) },
        onPinChannel = onPinChannel,
        onDismiss = onDismiss
    )
}
