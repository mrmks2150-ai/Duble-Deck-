package com.example.ui.browser

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleDeckScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Hardware back press handling:
    // If either deck can go back, or if maximized, handle back smoothly
    BackHandler(enabled = uiState.maximizedDeck != null || uiState.topDeck.canGoBack || uiState.bottomDeck.canGoBack) {
        if (uiState.maximizedDeck != null) {
            viewModel.restoreSplit()
        } else if (uiState.bottomDeck.canGoBack) {
            viewModel.goBack(DeckId.BOTTOM)
        } else if (uiState.topDeck.canGoBack) {
            viewModel.goBack(DeckId.TOP)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "DoubleDeck",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Presets Action
                    IconButton(
                        onClick = { viewModel.setShowPresetsSheet(true) },
                        modifier = Modifier.testTag("action_presets")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dual Presets",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Bookmarks Action
                    IconButton(
                        onClick = { viewModel.setShowBookmarksSheet(true, DeckId.TOP) },
                        modifier = Modifier.testTag("action_bookmarks")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Scratchpad / Note quick toggle on Bottom deck
                    IconButton(
                        onClick = {
                            val newMode = if (uiState.bottomDeck.viewMode == DeckViewMode.SCRATCHPAD) {
                                DeckViewMode.BROWSER
                            } else {
                                DeckViewMode.SCRATCHPAD
                            }
                            viewModel.setDeckViewMode(DeckId.BOTTOM, newMode)
                        },
                        modifier = Modifier.testTag("action_scratchpad")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Scratchpad",
                            tint = if (uiState.bottomDeck.viewMode == DeckViewMode.SCRATCHPAD) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Settings Action
                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                // Landscape: Side-by-side Dual Decks
                Row(modifier = Modifier.fillMaxSize()) {
                    // Deck 1
                    if (uiState.maximizedDeck == null || uiState.maximizedDeck == DeckId.TOP) {
                        Column(
                            modifier = Modifier
                                .weight(if (uiState.maximizedDeck == DeckId.TOP) 1f else uiState.splitRatio)
                                .fillMaxHeight()
                        ) {
                            DeckHeader(
                                deckState = uiState.topDeck,
                                isMaximized = uiState.maximizedDeck == DeckId.TOP,
                                onInputUrlChange = { viewModel.onInputUrlChanged(DeckId.TOP, it) },
                                onSubmitUrl = { viewModel.submitUrl(DeckId.TOP, it) },
                                onGoBack = { viewModel.goBack(DeckId.TOP) },
                                onGoForward = { viewModel.goForward(DeckId.TOP) },
                                onReload = { viewModel.reload(DeckId.TOP) },
                                onToggleDesktop = { viewModel.toggleDesktopMode(DeckId.TOP) },
                                onToggleBookmark = { viewModel.toggleBookmark(DeckId.TOP) },
                                onToggleMaximize = { viewModel.toggleMaximize(DeckId.TOP) },
                                onSendToOtherDeck = { viewModel.sendUrlToOtherDeck(DeckId.TOP) },
                                onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.TOP, DeckViewMode.SCRATCHPAD) },
                                onOpenBookmarks = { viewModel.setShowBookmarksSheet(true, DeckId.TOP) },
                                onOpenHistory = { viewModel.setShowHistorySheet(true, DeckId.TOP) }
                            )

                            if (uiState.topDeck.viewMode == DeckViewMode.SCRATCHPAD) {
                                ScratchpadView(
                                    content = uiState.noteContent,
                                    onContentChange = { viewModel.updateNoteContent(it) },
                                    onSwitchToBrowser = { viewModel.setDeckViewMode(DeckId.TOP, DeckViewMode.BROWSER) }
                                )
                            } else {
                                DeckWebView(
                                    deckState = uiState.topDeck,
                                    onPageStarted = { viewModel.onPageStarted(DeckId.TOP, it) },
                                    onPageFinished = { url, title, canBack, canFwd ->
                                        viewModel.onPageFinished(DeckId.TOP, url, title, canBack, canFwd)
                                    },
                                    onProgressChanged = { viewModel.onProgressChanged(DeckId.TOP, it) },
                                    onLaunchUrl = { viewModel.loadUrlInDeck(DeckId.TOP, it) },
                                    onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.TOP, DeckViewMode.SCRATCHPAD) }
                                )
                            }
                        }
                    }

                    // Divider (if split mode)
                    if (uiState.maximizedDeck == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(onClick = { viewModel.swapDecks() }) {
                                    Icon(Icons.Default.ViewStream, contentDescription = "Swap")
                                }
                            }
                        }
                    }

                    // Deck 2
                    if (uiState.maximizedDeck == null || uiState.maximizedDeck == DeckId.BOTTOM) {
                        Column(
                            modifier = Modifier
                                .weight(if (uiState.maximizedDeck == DeckId.BOTTOM) 1f else (1f - uiState.splitRatio))
                                .fillMaxHeight()
                        ) {
                            DeckHeader(
                                deckState = uiState.bottomDeck,
                                isMaximized = uiState.maximizedDeck == DeckId.BOTTOM,
                                onInputUrlChange = { viewModel.onInputUrlChanged(DeckId.BOTTOM, it) },
                                onSubmitUrl = { viewModel.submitUrl(DeckId.BOTTOM, it) },
                                onGoBack = { viewModel.goBack(DeckId.BOTTOM) },
                                onGoForward = { viewModel.goForward(DeckId.BOTTOM) },
                                onReload = { viewModel.reload(DeckId.BOTTOM) },
                                onToggleDesktop = { viewModel.toggleDesktopMode(DeckId.BOTTOM) },
                                onToggleBookmark = { viewModel.toggleBookmark(DeckId.BOTTOM) },
                                onToggleMaximize = { viewModel.toggleMaximize(DeckId.BOTTOM) },
                                onSendToOtherDeck = { viewModel.sendUrlToOtherDeck(DeckId.BOTTOM) },
                                onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.BOTTOM, DeckViewMode.SCRATCHPAD) },
                                onOpenBookmarks = { viewModel.setShowBookmarksSheet(true, DeckId.BOTTOM) },
                                onOpenHistory = { viewModel.setShowHistorySheet(true, DeckId.BOTTOM) }
                            )

                            if (uiState.bottomDeck.viewMode == DeckViewMode.SCRATCHPAD) {
                                ScratchpadView(
                                    content = uiState.noteContent,
                                    onContentChange = { viewModel.updateNoteContent(it) },
                                    onSwitchToBrowser = { viewModel.setDeckViewMode(DeckId.BOTTOM, DeckViewMode.BROWSER) }
                                )
                            } else {
                                DeckWebView(
                                    deckState = uiState.bottomDeck,
                                    onPageStarted = { viewModel.onPageStarted(DeckId.BOTTOM, it) },
                                    onPageFinished = { url, title, canBack, canFwd ->
                                        viewModel.onPageFinished(DeckId.BOTTOM, url, title, canBack, canFwd)
                                    },
                                    onProgressChanged = { viewModel.onProgressChanged(DeckId.BOTTOM, it) },
                                    onLaunchUrl = { viewModel.loadUrlInDeck(DeckId.BOTTOM, it) },
                                    onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.BOTTOM, DeckViewMode.SCRATCHPAD) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait: Top and Bottom Double Deck
                Column(modifier = Modifier.fillMaxSize()) {
                    // Deck 1 (Top Deck)
                    if (uiState.maximizedDeck == null || uiState.maximizedDeck == DeckId.TOP) {
                        Column(
                            modifier = Modifier
                                .weight(if (uiState.maximizedDeck == DeckId.TOP) 1f else uiState.splitRatio)
                                .fillMaxWidth()
                        ) {
                            DeckHeader(
                                deckState = uiState.topDeck,
                                isMaximized = uiState.maximizedDeck == DeckId.TOP,
                                onInputUrlChange = { viewModel.onInputUrlChanged(DeckId.TOP, it) },
                                onSubmitUrl = { viewModel.submitUrl(DeckId.TOP, it) },
                                onGoBack = { viewModel.goBack(DeckId.TOP) },
                                onGoForward = { viewModel.goForward(DeckId.TOP) },
                                onReload = { viewModel.reload(DeckId.TOP) },
                                onToggleDesktop = { viewModel.toggleDesktopMode(DeckId.TOP) },
                                onToggleBookmark = { viewModel.toggleBookmark(DeckId.TOP) },
                                onToggleMaximize = { viewModel.toggleMaximize(DeckId.TOP) },
                                onSendToOtherDeck = { viewModel.sendUrlToOtherDeck(DeckId.TOP) },
                                onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.TOP, DeckViewMode.SCRATCHPAD) },
                                onOpenBookmarks = { viewModel.setShowBookmarksSheet(true, DeckId.TOP) },
                                onOpenHistory = { viewModel.setShowHistorySheet(true, DeckId.TOP) }
                            )

                            if (uiState.topDeck.viewMode == DeckViewMode.SCRATCHPAD) {
                                ScratchpadView(
                                    content = uiState.noteContent,
                                    onContentChange = { viewModel.updateNoteContent(it) },
                                    onSwitchToBrowser = { viewModel.setDeckViewMode(DeckId.TOP, DeckViewMode.BROWSER) }
                                )
                            } else {
                                DeckWebView(
                                    deckState = uiState.topDeck,
                                    onPageStarted = { viewModel.onPageStarted(DeckId.TOP, it) },
                                    onPageFinished = { url, title, canBack, canFwd ->
                                        viewModel.onPageFinished(DeckId.TOP, url, title, canBack, canFwd)
                                    },
                                    onProgressChanged = { viewModel.onProgressChanged(DeckId.TOP, it) },
                                    onLaunchUrl = { viewModel.loadUrlInDeck(DeckId.TOP, it) },
                                    onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.TOP, DeckViewMode.SCRATCHPAD) }
                                )
                            }
                        }
                    }

                    // Divider Bar (Only shown when not maximized)
                    if (uiState.maximizedDeck == null) {
                        DividerBar(
                            splitRatio = uiState.splitRatio,
                            isSyncScroll = uiState.isSyncScrollEnabled,
                            onRatioChange = { viewModel.setSplitRatio(it) },
                            onSwapDecks = { viewModel.swapDecks() },
                            onToggleSyncScroll = { viewModel.toggleSyncScroll() },
                            onOpenPresets = { viewModel.setShowPresetsSheet(true) },
                            onSaveCurrentPreset = { viewModel.setShowSavePresetDialog(true) }
                        )
                    }

                    // Deck 2 (Bottom Deck)
                    if (uiState.maximizedDeck == null || uiState.maximizedDeck == DeckId.BOTTOM) {
                        Column(
                            modifier = Modifier
                                .weight(if (uiState.maximizedDeck == DeckId.BOTTOM) 1f else (1f - uiState.splitRatio))
                                .fillMaxWidth()
                        ) {
                            DeckHeader(
                                deckState = uiState.bottomDeck,
                                isMaximized = uiState.maximizedDeck == DeckId.BOTTOM,
                                onInputUrlChange = { viewModel.onInputUrlChanged(DeckId.BOTTOM, it) },
                                onSubmitUrl = { viewModel.submitUrl(DeckId.BOTTOM, it) },
                                onGoBack = { viewModel.goBack(DeckId.BOTTOM) },
                                onGoForward = { viewModel.goForward(DeckId.BOTTOM) },
                                onReload = { viewModel.reload(DeckId.BOTTOM) },
                                onToggleDesktop = { viewModel.toggleDesktopMode(DeckId.BOTTOM) },
                                onToggleBookmark = { viewModel.toggleBookmark(DeckId.BOTTOM) },
                                onToggleMaximize = { viewModel.toggleMaximize(DeckId.BOTTOM) },
                                onSendToOtherDeck = { viewModel.sendUrlToOtherDeck(DeckId.BOTTOM) },
                                onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.BOTTOM, DeckViewMode.SCRATCHPAD) },
                                onOpenBookmarks = { viewModel.setShowBookmarksSheet(true, DeckId.BOTTOM) },
                                onOpenHistory = { viewModel.setShowHistorySheet(true, DeckId.BOTTOM) }
                            )

                            if (uiState.bottomDeck.viewMode == DeckViewMode.SCRATCHPAD) {
                                ScratchpadView(
                                    content = uiState.noteContent,
                                    onContentChange = { viewModel.updateNoteContent(it) },
                                    onSwitchToBrowser = { viewModel.setDeckViewMode(DeckId.BOTTOM, DeckViewMode.BROWSER) }
                                )
                            } else {
                                DeckWebView(
                                    deckState = uiState.bottomDeck,
                                    onPageStarted = { viewModel.onPageStarted(DeckId.BOTTOM, it) },
                                    onPageFinished = { url, title, canBack, canFwd ->
                                        viewModel.onPageFinished(DeckId.BOTTOM, url, title, canBack, canFwd)
                                    },
                                    onProgressChanged = { viewModel.onProgressChanged(DeckId.BOTTOM, it) },
                                    onLaunchUrl = { viewModel.loadUrlInDeck(DeckId.BOTTOM, it) },
                                    onOpenScratchpad = { viewModel.setDeckViewMode(DeckId.BOTTOM, DeckViewMode.SCRATCHPAD) }
                                )
                            }
                        }
                    }
                }
            }

            // Restore Split Floating Action button when either deck is maximized
            if (uiState.maximizedDeck != null) {
                ElevatedButton(
                    onClick = { viewModel.restoreSplit() },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Exit Fullscreen (Split Deck)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Modal Sheets and Dialogs
    if (uiState.showPresetsSheet) {
        PresetsSheet(
            presets = presets,
            onSelectPreset = { topUrl, bottomUrl ->
                viewModel.loadPreset(topUrl, bottomUrl)
            },
            onDeletePreset = { viewModel.deletePreset(it) },
            onSaveCurrentAsPreset = {
                viewModel.setShowPresetsSheet(false)
                viewModel.setShowSavePresetDialog(true)
            },
            onDismiss = { viewModel.setShowPresetsSheet(false) }
        )
    }

    if (uiState.showBookmarksSheet || uiState.showHistorySheet) {
        BookmarksHistorySheet(
            initialTab = if (uiState.showHistorySheet) 1 else 0,
            activeDeck = uiState.activeDeckForSheets,
            bookmarks = bookmarks,
            history = history,
            onOpenUrl = { deck, url ->
                viewModel.loadUrlInDeck(deck, url)
            },
            onDeleteBookmark = { viewModel.deleteBookmark(it) },
            onDeleteHistory = { viewModel.deleteHistory(it) },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = {
                viewModel.setShowBookmarksSheet(false)
                viewModel.setShowHistorySheet(false)
            }
        )
    }

    if (uiState.showSavePresetDialog) {
        SavePresetDialog(
            topUrl = uiState.topDeck.currentUrl,
            bottomUrl = uiState.bottomDeck.currentUrl,
            onSave = { name, desc ->
                viewModel.saveCurrentAsPreset(name, desc)
            },
            onDismiss = { viewModel.setShowSavePresetDialog(false) }
        )
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            currentSearchEngine = uiState.searchEngine,
            onSelectSearchEngine = { viewModel.setSearchEngine(it) },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }
}
