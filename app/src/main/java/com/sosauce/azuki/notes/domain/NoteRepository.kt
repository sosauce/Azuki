package com.sosauce.azuki.notes.domain

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /** Notes that are not in the trash. */
    fun observeNotes(): Flow<List<Note>>
    fun observeTrashedNotes(): Flow<List<Note>>
    fun observeNote(id: Long): Flow<Note?>
    suspend fun getNote(id: Long): Note?

    /** Inserts the note when [Note.id] is 0, updates it otherwise. Tags are ignored. Returns the note id. */
    suspend fun saveNote(note: Note): Long
    suspend fun setTags(noteId: Long, tagIds: Set<Long>)
    suspend fun setPinned(ids: Collection<Long>, pinned: Boolean)
    suspend fun moveToTrash(ids: Collection<Long>)
    suspend fun restore(ids: Collection<Long>)
    suspend fun deleteForever(ids: Collection<Long>)
    suspend fun emptyTrash()
}

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>

    /** Returns the id of the tag with that name, creating it if needed. */
    suspend fun createTag(name: String): Long

    /** Returns false when another tag already has that name. */
    suspend fun renameTag(id: Long, name: String): Boolean
    suspend fun deleteTag(id: Long)
}
