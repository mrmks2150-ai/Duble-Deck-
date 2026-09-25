package com.example.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.DualPresetEntity
import com.example.data.local.HistoryEntity
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BrowserRepository(db)
    }

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presets: StateFlow<List<DualPresetEntity>> = repository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect latest note
        viewModelScope.launch {
            repository.latestNote.collect { note ->
                if (note != null) {
                    _uiState.update { it.copy(noteContent = note.content) }
                }
            }
        }
    }

    fun onInputUrlChanged(deckId: DeckId, input: String) {
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(topDeck = state.topDeck.copy(inputUrl = input))
            } else {
                state.copy(bottomDeck = state.bottomDeck.copy(inputUrl = input))
            }
        }
    }

    fun submitUrl(deckId: DeckId, rawInput: String) {
        val currentEngine = _uiState.value.searchEngine
        val resolved = UrlUtils.resolveUrl(rawInput, currentEngine)
        loadUrlInDeck(deckId, resolved)
    }

    fun loadUrlInDeck(deckId: DeckId, url: String) {
        val trigger = System.currentTimeMillis()
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(
                    topDeck = state.topDeck.copy(
                        targetUrlToLoad = url,
                        loadUrlTrigger = trigger,
                        inputUrl = url,
                        viewMode = DeckViewMode.BROWSER
                    )
                )
            } else {
                state.copy(
                    bottomDeck = state.bottomDeck.copy(
                        targetUrlToLoad = url,
                        loadUrlTrigger = trigger,
                        inputUrl = url,
                        viewMode = DeckViewMode.BROWSER
                    )
                )
            }
        }
        checkBookmarkStatus(deckId, url)
    }

    fun onPageStarted(deckId: DeckId, url: String) {
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(
                    topDeck = state.topDeck.copy(
                        currentUrl = url,
                        inputUrl = url,
                        isLoading = true,
                        progress = 10
                    )
                )
            } else {
                state.copy(
                    bottomDeck = state.bottomDeck.copy(
                        currentUrl = url,
                        inputUrl = url,
                        isLoading = true,
                        progress = 10
                    )
                )
            }
        }
    }

    fun onPageFinished(
        deckId: DeckId,
        url: String,
        title: String?,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        val resolvedTitle = if (title.isNullOrBlank()) url else title
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(
                    topDeck = state.topDeck.copy(
                        currentUrl = url,
                        inputUrl = url,
                        title = resolvedTitle,
                        isLoading = false,
                        progress = 100,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward
                    )
                )
            } else {
                state.copy(
                    bottomDeck = state.bottomDeck.copy(
                        currentUrl = url,
                        inputUrl = url,
                        title = resolvedTitle,
                        isLoading = false,
                        progress = 100,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward
                    )
                )
            }
        }

        // Record history in background
        viewModelScope.launch {
            repository.recordHistory(
                title = resolvedTitle,
                url = url,
                deck = if (deckId == DeckId.TOP) "A" else "B"
            )
            checkBookmarkStatus(deckId, url)
        }
    }

    fun onProgressChanged(deckId: DeckId, progress: Int) {
        _uiState.update { state ->
            val isLoading = progress < 100
            if (deckId == DeckId.TOP) {
                state.copy(
                    topDeck = state.topDeck.copy(
                        progress = progress,
                        isLoading = isLoading
                    )
                )
            } else {
                state.copy(
                    bottomDeck = state.bottomDeck.copy(
                        progress = progress,
                        isLoading = isLoading
                    )
                )
            }
        }
    }

    fun goBack(deckId: DeckId) {
        val trigger = System.currentTimeMillis()
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(topDeck = state.topDeck.copy(goBackTrigger = trigger))
            } else {
                state.copy(bottomDeck = state.bottomDeck.copy(goBackTrigger = trigger))
            }
        }
    }

    fun goForward(deckId: DeckId) {
        val trigger = System.currentTimeMillis()
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(topDeck = state.topDeck.copy(goForwardTrigger = trigger))
            } else {
                state.copy(bottomDeck = state.bottomDeck.copy(goForwardTrigger = trigger))
            }
        }
    }

    fun reload(deckId: DeckId) {
        val trigger = System.currentTimeMillis()
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(topDeck = state.topDeck.copy(reloadTrigger = trigger))
            } else {
                state.copy(bottomDeck = state.bottomDeck.copy(reloadTrigger = trigger))
            }
        }
    }

    fun toggleDesktopMode(deckId: DeckId) {
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                val newMode = !state.topDeck.isDesktopMode
                state.copy(topDeck = state.topDeck.copy(isDesktopMode = newMode, reloadTrigger = System.currentTimeMillis()))
            } else {
                val newMode = !state.bottomDeck.isDesktopMode
                state.copy(bottomDeck = state.bottomDeck.copy(isDesktopMode = newMode, reloadTrigger = System.currentTimeMillis()))
            }
        }
    }

    fun toggleBookmark(deckId: DeckId) {
        val deckState = if (deckId == DeckId.TOP) _uiState.value.topDeck else _uiState.value.bottomDeck
        val url = deckState.currentUrl
        if (url.isBlank()) return

        viewModelScope.launch {
            val isAlready = repository.isBookmarked(url)
            if (isAlready) {
                repository.removeBookmarkByUrl(url)
                updateBookmarkUi(deckId, false)
            } else {
                repository.addBookmark(
                    title = deckState.title.ifBlank { url },
                    url = url
                )
                updateBookmarkUi(deckId, true)
            }
        }
    }

    private fun checkBookmarkStatus(deckId: DeckId, url: String) {
        viewModelScope.launch {
            val isBookmarked = repository.isBookmarked(url)
            updateBookmarkUi(deckId, isBookmarked)
        }
    }

    private fun updateBookmarkUi(deckId: DeckId, isBookmarked: Boolean) {
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(topDeck = state.topDeck.copy(isBookmarked = isBookmarked))
            } else {
                state.copy(bottomDeck = state.bottomDeck.copy(isBookmarked = isBookmarked))
            }
        }
    }

    fun swapDecks() {
        _uiState.update { state ->
            val oldTop = state.topDeck
            val oldBottom = state.bottomDeck
            state.copy(
                topDeck = oldBottom.copy(
                    deckId = DeckId.TOP,
                    loadUrlTrigger = System.currentTimeMillis(),
                    targetUrlToLoad = oldBottom.currentUrl
                ),
                bottomDeck = oldTop.copy(
                    deckId = DeckId.BOTTOM,
                    loadUrlTrigger = System.currentTimeMillis(),
                    targetUrlToLoad = oldTop.currentUrl
                )
            )
        }
    }

    fun sendUrlToOtherDeck(sourceDeck: DeckId) {
        val sourceState = if (sourceDeck == DeckId.TOP) _uiState.value.topDeck else _uiState.value.bottomDeck
        val targetDeck = if (sourceDeck == DeckId.TOP) DeckId.BOTTOM else DeckId.TOP
        if (sourceState.currentUrl.isNotBlank()) {
            loadUrlInDeck(targetDeck, sourceState.currentUrl)
        }
    }

    fun setSplitRatio(ratio: Float) {
        val clamped = ratio.coerceIn(0.18f, 0.82f)
        _uiState.update { it.copy(splitRatio = clamped, maximizedDeck = null) }
    }

    fun toggleMaximize(deckId: DeckId) {
        _uiState.update { state ->
            if (state.maximizedDeck == deckId) {
                state.copy(maximizedDeck = null)
            } else {
                state.copy(maximizedDeck = deckId)
            }
        }
    }

    fun restoreSplit() {
        _uiState.update { it.copy(maximizedDeck = null) }
    }

    fun toggleSyncScroll() {
        _uiState.update { it.copy(isSyncScrollEnabled = !it.isSyncScrollEnabled) }
    }

    fun setDeckViewMode(deckId: DeckId, mode: DeckViewMode) {
        _uiState.update { state ->
            if (deckId == DeckId.TOP) {
                state.copy(topDeck = state.topDeck.copy(viewMode = mode))
            } else {
                state.copy(bottomDeck = state.bottomDeck.copy(viewMode = mode))
            }
        }
    }

    fun loadPreset(topUrl: String, bottomUrl: String) {
        loadUrlInDeck(DeckId.TOP, topUrl)
        loadUrlInDeck(DeckId.BOTTOM, bottomUrl)
        _uiState.update { it.copy(showPresetsSheet = false, maximizedDeck = null) }
    }

    fun saveCurrentAsPreset(name: String, description: String) {
        val topUrl = _uiState.value.topDeck.currentUrl
        val bottomUrl = _uiState.value.bottomDeck.currentUrl
        if (name.isBlank() || topUrl.isBlank() || bottomUrl.isBlank()) return

        viewModelScope.launch {
            repository.savePreset(
                name = name,
                topUrl = topUrl,
                bottomUrl = bottomUrl,
                description = description
            )
            _uiState.update { it.copy(showSavePresetDialog = false) }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            repository.deletePreset(id)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
            // Re-evaluate current urls
            checkBookmarkStatus(DeckId.TOP, _uiState.value.topDeck.currentUrl)
            checkBookmarkStatus(DeckId.BOTTOM, _uiState.value.bottomDeck.currentUrl)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun updateNoteContent(content: String) {
        _uiState.update { it.copy(noteContent = content) }
        viewModelScope.launch {
            repository.saveNote(content)
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        _uiState.update { it.copy(searchEngine = engine) }
    }

    fun setShowPresetsSheet(show: Boolean) {
        _uiState.update { it.copy(showPresetsSheet = show) }
    }

    fun setShowBookmarksSheet(show: Boolean, targetDeck: DeckId = DeckId.TOP) {
        _uiState.update { it.copy(showBookmarksSheet = show, activeDeckForSheets = targetDeck) }
    }

    fun setShowHistorySheet(show: Boolean, targetDeck: DeckId = DeckId.TOP) {
        _uiState.update { it.copy(showHistorySheet = show, activeDeckForSheets = targetDeck) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowSavePresetDialog(show: Boolean) {
        _uiState.update { it.copy(showSavePresetDialog = show) }
    }
}
