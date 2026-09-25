package com.example.ui.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// Standard Chrome Mobile User-Agent that works with YouTube, Google, and modern video streaming sites
private const val CHROME_MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DeckWebView(
    deckState: DeckUiState,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?, Boolean, Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onLaunchUrl: (String) -> Unit,
    onOpenScratchpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageError by remember { mutableStateOf<String?>(null) }

    // State for Fullscreen Video / CustomView (HTML5 & YouTube video fullscreen)
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // React to triggers
    LaunchedEffect(deckState.loadUrlTrigger) {
        val target = deckState.targetUrlToLoad
        if (!target.isNullOrBlank()) {
            pageError = null
            webViewRef?.loadUrl(target)
        }
    }

    LaunchedEffect(deckState.reloadTrigger) {
        if (deckState.reloadTrigger > 0L) {
            webViewRef?.let { webView ->
                val settings = webView.settings
                if (deckState.isDesktopMode) {
                    settings.userAgentString = DESKTOP_USER_AGENT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                } else {
                    settings.userAgentString = CHROME_MOBILE_USER_AGENT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                }
                pageError = null
                webView.reload()
            }
        }
    }

    LaunchedEffect(deckState.goBackTrigger) {
        if (deckState.goBackTrigger > 0L) {
            if (customVideoView != null) {
                customViewCallback?.onCustomViewHidden()
                customVideoView = null
                customViewCallback = null
            } else if (webViewRef?.canGoBack() == true) {
                pageError = null
                webViewRef?.goBack()
            }
        }
    }

    LaunchedEffect(deckState.goForwardTrigger) {
        if (deckState.goForwardTrigger > 0L && webViewRef?.canGoForward() == true) {
            pageError = null
            webViewRef?.goForward()
        }
    }

    // Back handler for custom full-screen video
    BackHandler(enabled = customVideoView != null) {
        customViewCallback?.onCustomViewHidden()
        customVideoView = null
        customViewCallback = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable Hardware Acceleration for video playback
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    // Cookie Configuration: YouTube and video sites require third party cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        allowContentAccess = true
                        allowFileAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        setSupportMultipleWindows(true)
                        javaScriptCanOpenWindowsAutomatically = true

                        // Use clean standard Chrome user agent so YouTube and video platforms render properly
                        userAgentString = if (deckState.isDesktopMode) DESKTOP_USER_AGENT else CHROME_MOBILE_USER_AGENT
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val urlStr = uri.toString()
                            val scheme = uri.scheme?.lowercase() ?: ""

                            // Handle intent:// urls (often generated by YouTube mobile to open the app)
                            if (scheme == "intent") {
                                try {
                                    val parsedIntent = Intent.parseUri(urlStr, Intent.URI_INTENT_SCHEME)
                                    val fallbackUrl = parsedIntent.getStringExtra("browser_fallback_url")
                                    if (!fallbackUrl.isNullOrBlank()) {
                                        view?.loadUrl(fallbackUrl)
                                        return true
                                    }
                                    val dataUri = parsedIntent.data
                                    if (dataUri != null && (dataUri.scheme == "http" || dataUri.scheme == "https")) {
                                        view?.loadUrl(dataUri.toString())
                                        return true
                                    }
                                } catch (_: Exception) {
                                    // fallback
                                }
                                return true
                            }

                            // Keep all standard web pages inside our browser deck
                            if (scheme == "http" || scheme == "https") {
                                return false
                            }

                            // Handle external schemes safely
                            return try {
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                ctx.startActivity(intent)
                                true
                            } catch (_: Exception) {
                                true
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            pageError = null
                            if (url != null) {
                                onPageStarted(url)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (url != null) {
                                onPageFinished(
                                    url,
                                    view?.title,
                                    view?.canGoBack() == true,
                                    view?.canGoForward() == true
                                )
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                pageError = error?.description?.toString() ?: "Failed to load page"
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChanged(newProgress)
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            val current = view?.url ?: ""
                            onPageFinished(
                                current,
                                title,
                                view?.canGoBack() == true,
                                view?.canGoForward() == true
                            )
                        }

                        // Handle Video Fullscreen / HTML5 Video View
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customVideoView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customViewCallback?.onCustomViewHidden()
                            customVideoView = null
                            customViewCallback = null
                        }

                        override fun getDefaultVideoPoster(): Bitmap? {
                            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        }

                        // Grant permission requests (e.g. for media playback / DRM)
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            request?.grant(request.resources)
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            val newWebView = WebView(ctx)
                            newWebView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    subView: WebView?,
                                    subRequest: WebResourceRequest?
                                ): Boolean {
                                    val subUrl = subRequest?.url?.toString()
                                    if (!subUrl.isNullOrBlank()) {
                                        view?.loadUrl(subUrl)
                                    }
                                    return true
                                }
                            }
                            transport?.webView = newWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }

                    webViewRef = this

                    if (deckState.currentUrl.isNotBlank() && deckState.currentUrl != "about:blank") {
                        loadUrl(deckState.currentUrl)
                    }
                }
            },
            update = { webView ->
                webViewRef = webView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Fullscreen Video Overlay (e.g. YouTube fullscreen)
        if (customVideoView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        FrameLayout(it).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            val v = customVideoView
                            if (v?.parent is ViewGroup) {
                                (v.parent as ViewGroup).removeView(v)
                            }
                            if (v != null) {
                                addView(v)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close/Exit Fullscreen Button
                FilledTonalButton(
                    onClick = {
                        customViewCallback?.onCustomViewHidden()
                        customVideoView = null
                        customViewCallback = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Exit Video", fontSize = 12.sp)
                }
            }
        }

        // Loading indicator
        AnimatedVisibility(
            visible = deckState.isLoading && deckState.progress in 1..99,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { deckState.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Error overlay with Retry
        if (pageError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Page could not be loaded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pageError ?: "Network or address error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            pageError = null
                            webViewRef?.reload()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }

        // Empty state / New Tab Launch Screen when URL is empty
        if (deckState.currentUrl.isBlank() || deckState.currentUrl == "about:blank") {
            DeckStartScreen(
                deckId = deckState.deckId,
                onSearch = onLaunchUrl,
                onQuickLaunch = onLaunchUrl,
                onOpenScratchpad = onOpenScratchpad,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (customVideoView != null) {
                customViewCallback?.onCustomViewHidden()
                customVideoView = null
                customViewCallback = null
            }
            webViewRef?.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckStartScreen(
    deckId: DeckId,
    onSearch: (String) -> Unit,
    onQuickLaunch: (String) -> Unit,
    onOpenScratchpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    val deckLabel = if (deckId == DeckId.TOP) "Deck 1 (Top)" else "Deck 2 (Bottom)"

    val shortcuts = listOf(
        QuickTile("YouTube", "https://www.youtube.com", Icons.Default.OndemandVideo, Color(0xFFE53935)),
        QuickTile("Google", "https://www.google.com", Icons.Default.Search, Color(0xFF4285F4)),
        QuickTile("Wikipedia", "https://en.m.wikipedia.org", Icons.Default.Language, Color(0xFF673AB7)),
        QuickTile("Reddit", "https://www.reddit.com", Icons.Default.Article, Color(0xFFFF5722)),
        QuickTile("GitHub", "https://github.com", Icons.Default.Code, Color(0xFF24292E)),
        QuickTile("Translate", "https://translate.google.com", Icons.Default.Translate, Color(0xFF00897B)),
        QuickTile("Shopping", "https://www.amazon.com", Icons.Default.ShoppingBag, Color(0xFFFF9800)),
        QuickTile("Hacker News", "https://news.ycombinator.com", Icons.Default.Public, Color(0xFFFF6600))
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Badge
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = deckLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "DoubleDeck Navigator",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Browse concurrently with dual screen multitasking",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Search Input
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = { Text("Search or type URL...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (queryText.isNotBlank()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSearch(queryText) }
                            .padding(8.dp)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deck_start_input_${deckId.name.lowercase()}")
        )

        // Quick Shortcuts
        Text(
            text = "Speed Dials",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            shortcuts.forEach { item ->
                ElevatedCard(
                    onClick = { onQuickLaunch(item.url) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_tile_${item.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = item.color.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name,
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live Scratchpad card
        OutlinedCardWithAction(
            title = "Open Scratchpad Note",
            description = "Switch this deck into a research notepad",
            icon = Icons.Default.EditNote,
            onClick = onOpenScratchpad
        )
    }
}

private data class QuickTile(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun OutlinedCardWithAction(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
