package com.sosauce.azuki.core.di

import androidx.room.Room
import com.sosauce.azuki.core.preferences.SettingsRepository
import com.sosauce.azuki.notes.data.RoomNoteRepository
import com.sosauce.azuki.notes.data.RoomTagRepository
import com.sosauce.azuki.notes.data.local.AzukiDatabase
import com.sosauce.azuki.notes.domain.NoteRepository
import com.sosauce.azuki.notes.domain.TagRepository
import com.sosauce.azuki.notes.presentation.detail.NoteDetailViewModel
import com.sosauce.azuki.notes.presentation.list.NoteListViewModel
import com.sosauce.azuki.notes.presentation.trash.TrashViewModel
import com.sosauce.azuki.settings.SettingsViewModel
import com.sosauce.azuki.settings.tags.ManageTagsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AzukiDatabase::class.java,
            name = AzukiDatabase.NAME
        ).build()
    }
    single { get<AzukiDatabase>().noteDao }
    single { get<AzukiDatabase>().tagDao }
    /** Outlives ViewModels, used to flush pending saves when a screen is closed. */
    single { CoroutineScope(SupervisorJob()) }

    singleOf(::SettingsRepository)
    singleOf(::RoomNoteRepository) { bind<NoteRepository>() }
    singleOf(::RoomTagRepository) { bind<TagRepository>() }
}

val presentationModule = module {
    viewModelOf(::NoteListViewModel)
    viewModelOf(::TrashViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ManageTagsViewModel)
    viewModel { (noteId: Long) ->
        NoteDetailViewModel(
            noteId = noteId,
            noteRepository = get(),
            tagRepository = get(),
            settingsRepository = get(),
            appScope = get(),
            savedStateHandle = get()
        )
    }
}
