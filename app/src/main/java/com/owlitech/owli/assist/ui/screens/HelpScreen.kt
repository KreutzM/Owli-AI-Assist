package com.owlitech.owli.assist.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.owlitech.owli.assist.util.AppLinks
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier
) {
    AssetDocumentScreen(
        assetFolder = "help",
        modifier = modifier
    )
}

@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier
) {
    AssetDocumentScreen(
        assetFolder = "privacy-policy",
        modifier = modifier
    )
}

@Composable
private fun AssetDocumentScreen(
    assetFolder: String,
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
    val documentUrl = if (language.startsWith("de")) {
        "file:///android_asset/$assetFolder/de/index.html"
    } else {
        "file:///android_asset/$assetFolder/en/index.html"
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
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url == AppLinks.PRIVACY_POLICY_URL || url.startsWith("http://") || url.startsWith("https://")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            return true
                        }
                        return false
                    }
                }
                loadUrl(documentUrl)
            }
        },
        update = { webView ->
            if (webView.settings.textZoom != textZoom) {
                webView.settings.textZoom = textZoom
            }
            if (webView.url != documentUrl) {
                webView.loadUrl(documentUrl)
            }
        }
    )
}
