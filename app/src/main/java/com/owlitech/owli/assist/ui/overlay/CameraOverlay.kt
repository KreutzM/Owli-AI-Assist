package com.owlitech.owli.assist.ui.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.dp

object CameraOverlayDefaults {
    val textColor: Color = Color.White
    val scrimColor: Color = Color.Black.copy(alpha = 0.6f)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
}

@Composable
fun CameraOverlayScope(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides CameraOverlayDefaults.textColor) {
        Surface(
            color = Color.Transparent,
            contentColor = CameraOverlayDefaults.textColor,
            modifier = modifier
        ) {
            content()
        }
    }
}

@Composable
fun CameraOverlayLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CameraOverlayDefaults.scrimColor,
        contentColor = CameraOverlayDefaults.textColor,
        shape = CameraOverlayDefaults.shape,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = CameraOverlayDefaults.textColor,
            modifier = Modifier.padding(CameraOverlayDefaults.padding)
        )
    }
}

@Composable
fun CameraOverlayRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = CameraOverlayDefaults.scrimColor,
        contentColor = CameraOverlayDefaults.textColor,
        shape = CameraOverlayDefaults.shape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(CameraOverlayDefaults.padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
