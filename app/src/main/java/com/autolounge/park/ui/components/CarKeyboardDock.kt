package com.autolounge.park.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autolounge.park.i18n.tr
import com.autolounge.park.data.AutoLoungeSyncBridge
import com.autolounge.park.ui.AutoLoungePalette

@Composable
fun CarKeyboardDock(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }

    fun send() {
        if (text.isNotBlank()) {
            AutoLoungeSyncBridge.sendRemoteText(text)
            text = ""
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AutoLoungePalette.CarbonElevated.copy(alpha = 0.97f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Keyboard,
                contentDescription = null,
                tint = AutoLoungePalette.EmeraldNeon,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AutoLoungePalette.SurfaceCard)
                    .border(1.dp, AutoLoungePalette.SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    textStyle = TextStyle(color = AutoLoungePalette.TextPure, fontSize = 13.5.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (text.isEmpty()) {
                    Text(
                        tr("Araç ekranına yaz..."),
                        color = AutoLoungePalette.TextDim,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = { send() },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = tr("Araç ekranına gönder"),
                    tint = if (text.isNotBlank()) AutoLoungePalette.EmeraldNeon else AutoLoungePalette.TextDim,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
