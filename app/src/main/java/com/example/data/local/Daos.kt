package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 150")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

@Dao
interface DualPresetDao {
    @Query("SELECT * FROM dual_presets ORDER BY isCustom ASC, id ASC")
    fun getAllPresets(): Flow<List<DualPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: DualPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPresets(presets: List<DualPresetEntity>)

    @Query("DELETE FROM dual_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)
}

@Dao
interface ScratchNoteDao {
    @Query("SELECT * FROM scratch_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<ScratchNoteEntity>>

    @Query("SELECT * FROM scratch_notes ORDER BY updatedAt DESC LIMIT 1")
    fun getLatestNote(): Flow<ScratchNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ScratchNoteEntity): Long

    @Query("DELETE FROM scratch_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}
