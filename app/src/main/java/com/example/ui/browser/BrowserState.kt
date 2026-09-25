package com.example.ui.browser

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class DeckId {
    TOP, BOTTOM
}

enum class DeckViewMode {
    BROWSER, SCRATCHPAD
}

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q="),
    WIKIPEDIA("Wikipedia", "https://en.wikipedia.org/w/index.php?search=")
}

data class DeckUiState(
    val deckId: DeckId,
    val currentUrl: String = "",
    val inputUrl: String = "",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isBookmarked: Boolean = false,
    val viewMode: DeckViewMode = DeckViewMode.BROWSER,
    val reloadTrigger: Long = 0L,
    val goBackTrigger: Long = 0L,
    val goForwardTrigger: Long = 0L,
    val loadUrlTrigger: Long = 0L,
    val targetUrlToLoad: String? = null
)

data class BrowserUiState(
    val topDeck: DeckUiState = DeckUiState(
        deckId = DeckId.TOP,
        currentUrl = "https://www.google.com",
        inputUrl = "https://www.google.com",
        title = "Google"
    ),
    val bottomDeck: DeckUiState = DeckUiState(
        deckId = DeckId.BOTTOM,
        currentUrl = "https://en.m.wikipedia.org",
        inputUrl = "https://en.m.wikipedia.org",
        title = "Wikipedia"
    ),
    val splitRatio: Float = 0.5f,
    val maximizedDeck: DeckId? = null,
    val isSyncScrollEnabled: Boolean = false,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val activeDeckForSheets: DeckId = DeckId.TOP,
    val showPresetsSheet: Boolean = false,
    val showBookmarksSheet: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showSavePresetDialog: Boolean = false,
    val isJavascriptEnabled: Boolean = true,
    val noteContent: String = ""
)

object UrlUtils {
    fun resolveUrl(input: String, searchEngine: SearchEngine = SearchEngine.GOOGLE): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("http://") ||
            trimmed.startsWith("https://") ||
            trimmed.startsWith("file://") ||
            trimmed.startsWith("about:")
        ) {
            return trimmed
        }

        // Check if looks like domain name e.g. "en.wikipedia.org", "news.ycombinator.com/item?id=1"
        val domainRegex = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:[0-9]+)?(/.*)?$")
        if (!trimmed.contains(" ") && domainRegex.matches(trimmed)) {
            return "https://$trimmed"
        }

        // Otherwise fallback to search engine
        val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
        return "${searchEngine.searchUrl}$encoded"
    }
}
