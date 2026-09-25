package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.DualPresetEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.ScratchNoteEntity
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val db: AppDatabase) {

    val allBookmarks: Flow<List<BookmarkEntity>> = db.bookmarkDao().getAllBookmarks()
    val allHistory: Flow<List<HistoryEntity>> = db.historyDao().getAllHistory()
    val allPresets: Flow<List<DualPresetEntity>> = db.dualPresetDao().getAllPresets()
    val latestNote: Flow<ScratchNoteEntity?> = db.scratchNoteDao().getLatestNote()

    suspend fun addBookmark(title: String, url: String, category: String = "General") {
        db.bookmarkDao().insertBookmark(
            BookmarkEntity(
                title = if (title.isBlank()) url else title,
                url = url,
                category = category
            )
        )
    }

    suspend fun removeBookmark(id: Long) {
        db.bookmarkDao().deleteBookmarkById(id)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        db.bookmarkDao().deleteBookmarkByUrl(url)
    }

    suspend fun isBookmarked(url: String): Boolean {
        return db.bookmarkDao().isBookmarked(url)
    }

    suspend fun recordHistory(title: String, url: String, deck: String) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return
        db.historyDao().insertHistory(
            HistoryEntity(
                title = if (title.isBlank()) url else title,
                url = url,
                deck = deck
            )
        )
    }

    suspend fun deleteHistory(id: Long) {
        db.historyDao().deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        db.historyDao().clearAllHistory()
    }

    suspend fun savePreset(name: String, topUrl: String, bottomUrl: String, description: String = "") {
        db.dualPresetDao().insertPreset(
            DualPresetEntity(
                name = name,
                topUrl = topUrl,
                bottomUrl = bottomUrl,
                description = description,
                isCustom = true
            )
        )
    }

    suspend fun deletePreset(id: Long) {
        db.dualPresetDao().deletePresetById(id)
    }

    suspend fun saveNote(content: String, title: String = "Quick Note") {
        db.scratchNoteDao().insertNote(
            ScratchNoteEntity(
                id = 1L, // Always keep the main active scratchpad updated
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
