package com.sosauce.azuki.notes.data

import com.sosauce.azuki.notes.data.local.NoteDao
import com.sosauce.azuki.notes.data.local.TagDao
import com.sosauce.azuki.notes.data.local.TagEntity
import com.sosauce.azuki.notes.domain.Note
import com.sosauce.azuki.notes.domain.NoteRepository
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.azuki.notes.domain.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeNotes().map { notes -> notes.map { it.toNote() } }

    override fun observeTrashedNotes(): Flow<List<Note>> =
        noteDao.observeTrashedNotes().map { notes -> notes.map { it.toNote() } }

    override fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeNote(id).map { it?.toNote() }

    override suspend fun getNote(id: Long): Note? = noteDao.getNote(id)?.toNote()

    override suspend fun saveNote(note: Note): Long {
        return if (note.id == 0L) {
            noteDao.insert(note.toEntity())
        } else {
            noteDao.update(note.toEntity())
            note.id
        }
    }

    override suspend fun setTags(noteId: Long, tagIds: Set<Long>) = noteDao.setTags(noteId, tagIds)

    override suspend fun setPinned(ids: Collection<Long>, pinned: Boolean) =
        noteDao.setPinned(ids, pinned)

    override suspend fun moveToTrash(ids: Collection<Long>) =
        noteDao.moveToTrash(ids, System.currentTimeMillis())

    override suspend fun restore(ids: Collection<Long>) = noteDao.restore(ids)

    override suspend fun deleteForever(ids: Collection<Long>) = noteDao.deleteForever(ids)

    override suspend fun emptyTrash() = noteDao.emptyTrash()
}

class RoomTagRepository(
    private val tagDao: TagDao
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { tags -> tags.map { it.toTag() } }

    override suspend fun createTag(name: String): Long {
        val trimmed = name.trim()
        return tagDao.findByName(trimmed)?.id ?: tagDao.insert(TagEntity(name = trimmed))
    }

    override suspend fun renameTag(id: Long, name: String): Boolean {
        val trimmed = name.trim()
        val existing = tagDao.findByName(trimmed)
        if (existing != null && existing.id != id) return false
        tagDao.rename(id, trimmed)
        return true
    }

    override suspend fun deleteTag(id: Long) = tagDao.delete(id)
}
