package com.sosauce.azuki.notes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AzukiDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val tagDao: TagDao

    companion object {
        const val NAME = "azuki.db"
    }
}
