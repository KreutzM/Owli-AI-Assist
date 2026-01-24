package com.owlitech.owli.assist.ui.screens

import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val fontScale = configuration.fontScale
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locales = configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    } else {
        @Suppress("DEPRECATION")
        configuration.locale
    }
    val language = locale.language.lowercase(Locale.ROOT)
    val helpUrl = if (language.startsWith("de")) {
        "file:///android_asset/help/de/index.html"
    } else {
        "file:///android_asset/help/en/index.html"
    }
    val textZoom = (fontScale * 100).roundToInt()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.textZoom = textZoom
                settings.allowFileAccess = true
                webViewClient = WebViewClient()
                loadUrl(helpUrl)
            }
        },
        update = { webView ->
            if (webView.settings.textZoom != textZoom) {
                webView.settings.textZoom = textZoom
            }
            if (webView.url != helpUrl) {
                webView.loadUrl(helpUrl)
            }
        }
    )
}
