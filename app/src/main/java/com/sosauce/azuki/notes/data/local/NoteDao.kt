package com.sosauce.azuki.notes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE trashedAt IS NULL")
    abstract fun observeNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC")
    abstract fun observeTrashedNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    abstract fun observeNote(id: Long): Flow<NoteWithTags?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun getNote(id: Long): NoteWithTags?

    @Insert
    abstract suspend fun insert(note: NoteEntity): Long

    @Update
    abstract suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id IN (:ids)")
    abstract suspend fun setPinned(ids: Collection<Long>, pinned: Boolean)

    @Query("UPDATE notes SET trashedAt = :trashedAt, isPinned = 0 WHERE id IN (:ids)")
    abstract suspend fun moveToTrash(ids: Collection<Long>, trashedAt: Long)

    @Query("UPDATE notes SET trashedAt = NULL WHERE id IN (:ids)")
    abstract suspend fun restore(ids: Collection<Long>)

    @Query("DELETE FROM note_tags WHERE noteId IN (:ids)")
    abstract suspend fun deleteRefsForNotes(ids: Collection<Long>)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    abstract suspend fun deleteNotes(ids: Collection<Long>)

    @Query("SELECT id FROM notes WHERE trashedAt IS NOT NULL")
    abstract suspend fun getTrashedIds(): List<Long>

    @Transaction
    open suspend fun deleteForever(ids: Collection<Long>) {
        deleteRefsForNotes(ids)
        deleteNotes(ids)
    }

    @Transaction
    open suspend fun emptyTrash() {
        deleteForever(getTrashedIds())
    }

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    abstract suspend fun deleteRefsForNote(noteId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRefs(refs: List<NoteTagCrossRef>)

    @Transaction
    open suspend fun setTags(noteId: Long, tagIds: Set<Long>) {
        deleteRefsForNote(noteId)
        insertRefs(tagIds.map { NoteTagCrossRef(noteId = noteId, tagId = it) })
    }
}

@Dao
abstract class TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findByName(name: String): TagEntity?

    @Insert
    abstract suspend fun insert(tag: TagEntity): Long

    @Query("UPDATE tags SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM note_tags WHERE tagId = :id")
    abstract suspend fun deleteRefsForTag(id: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    abstract suspend fun deleteTagRow(id: Long)

    @Transaction
    open suspend fun delete(id: Long) {
        deleteRefsForTag(id)
        deleteTagRow(id)
    }
}
