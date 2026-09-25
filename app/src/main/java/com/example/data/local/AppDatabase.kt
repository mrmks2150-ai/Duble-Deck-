package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        DualPresetEntity::class,
        ScratchNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun dualPresetDao(): DualPresetDao
    abstract fun scratchNoteDao(): ScratchNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "double_deck_browser.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateStarterData(database)
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateStarterData(db: AppDatabase) {
            val starterPresets = listOf(
                DualPresetEntity(
                    name = "Search & Knowledge",
                    topUrl = "https://www.google.com",
                    bottomUrl = "https://en.m.wikipedia.org",
                    description = "Google Search on Top, Wikipedia on Bottom",
                    iconName = "search",
                    isCustom = false
                ),
                DualPresetEntity(
                    name = "Video & Social",
                    topUrl = "https://www.youtube.com",
                    bottomUrl = "https://www.reddit.com",
                    description = "Watch media while checking discussions",
                    iconName = "video",
                    isCustom = false
                ),
                DualPresetEntity(
                    name = "Tech & Documentation",
                    topUrl = "https://news.ycombinator.com",
                    bottomUrl = "https://developer.mozilla.org",
                    description = "Hacker News & MDN Web Docs",
                    iconName = "code",
                    isCustom = false
                ),
                DualPresetEntity(
                    name = "Compare Shopping",
                    topUrl = "https://www.amazon.com",
                    bottomUrl = "https://www.ebay.com",
                    description = "Compare prices side-by-side",
                    iconName = "shopping",
                    isCustom = false
                ),
                DualPresetEntity(
                    name = "Read & Translate",
                    topUrl = "https://en.m.wikipedia.org/wiki/Special:Random",
                    bottomUrl = "https://translate.google.com",
                    description = "Read international sites & live translate",
                    iconName = "translate",
                    isCustom = false
                )
            )
            db.dualPresetDao().insertPresets(starterPresets)

            // Starter bookmarks
            val starterBookmarks = listOf(
                BookmarkEntity(title = "YouTube", url = "https://www.youtube.com", category = "Media"),
                BookmarkEntity(title = "Google", url = "https://www.google.com", category = "Search"),
                BookmarkEntity(title = "Wikipedia", url = "https://en.m.wikipedia.org", category = "Reference"),
                BookmarkEntity(title = "Hacker News", url = "https://news.ycombinator.com", category = "Tech"),
                BookmarkEntity(title = "Reddit", url = "https://www.reddit.com", category = "Social"),
                BookmarkEntity(title = "DuckDuckGo", url = "https://duckduckgo.com", category = "Search"),
                BookmarkEntity(title = "GitHub", url = "https://github.com", category = "Development")
            )
            starterBookmarks.forEach { db.bookmarkDao().insertBookmark(it) }

            // Starter note
            db.scratchNoteDao().insertNote(
                ScratchNoteEntity(
                    title = "Research Scratchpad",
                    content = "# DoubleDeck Browser Scratchpad\n\n- Multitask between two live web decks.\n- Use the split bar to resize or swap top & bottom.\n- Tap Presets to launch paired workflows.\n- Send URLs across decks instantly!"
                )
            )
        }
    }
}
