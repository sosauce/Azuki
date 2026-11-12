package com.sosauce.azuki.notes.data

import com.sosauce.azuki.notes.data.local.NoteEntity
import com.sosauce.azuki.notes.data.local.NoteWithTags
import com.sosauce.azuki.notes.data.local.TagEntity
import com.sosauce.azuki.notes.domain.Note
import com.sosauce.azuki.notes.domain.Tag

fun NoteWithTags.toNote(): Note = Note(
    id = note.id,
    title = note.title,
    content = note.content,
    seedColor = note.seedColor,
    isPinned = note.isPinned,
    createdAt = note.createdAt,
    updatedAt = note.updatedAt,
    trashedAt = note.trashedAt,
    tags = tags.map { it.toTag() }.sortedBy { it.name.lowercase() }
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    seedColor = seedColor,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    trashedAt = trashedAt
)

fun TagEntity.toTag(): Tag = Tag(id = id, name = name)
