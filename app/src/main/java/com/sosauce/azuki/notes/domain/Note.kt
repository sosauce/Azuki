package com.sosauce.azuki.notes.domain

data class Note(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    /** ARGB seed the note's color scheme is generated from, null = follows the app theme. */
    val seedColor: Int? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val trashedAt: Long? = null,
    val tags: List<Tag> = emptyList()
) {
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()
}

data class Tag(
    val id: Long = 0L,
    val name: String
)

enum class NoteSort { UPDATED, CREATED, TITLE }
