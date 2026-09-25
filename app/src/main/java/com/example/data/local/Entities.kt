package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val deck: String = "A", // "A" or "B"
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dual_presets")
data class DualPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val topUrl: String,
    val bottomUrl: String,
    val description: String = "",
    val iconName: String = "split",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scratch_notes")
data class ScratchNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Quick Note",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
