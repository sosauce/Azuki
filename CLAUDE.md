# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Azuki is a single-module (`:app`) Jetpack Compose note-taking app (`com.sosauce.azuki`). Notes are stored locally in Room, written in Markdown, and organized with tags.

## Commands

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # install on a connected device/emulator
./gradlew lint                   # Android lint
```

Toolchain:
- AGP 9 with its built-in Kotlin support, so there is no `kotlin-android` plugin.
- KSP is used for Room.
- The Java target is 17.
- `compileSdk` and `targetSdk` are 37; `minSdk` is 26.
- `material3` is deliberately pinned to `1.5.0-rc01`, overriding the Compose BOM, so the Material 3 Expressive APIs are available. Versions live in `gradle/libs.versions.toml`.
- R8 keep rules go in `app/src/main/keepRules/`, not `proguard-rules.pro`.

## Architecture

Code is grouped by feature under `app/src/main/java/com/sosauce/azuki/`:

- `app/`: `AzukiApplication` (starts Koin), `MainActivity`, and Navigation 3 setup.
  - `navigation/Screen.kt` declares `@Serializable` `NavKey`s. `NoteDetail(noteId = 0)` means "new note".
  - `Navigation.kt` holds a hand-managed back stack in a `NavDisplay`, with the saveable-state and ViewModel-store entry decorators.
- `core/`:
  - `di/AppModule.kt` defines two Koin modules, `dataModule` and `presentationModule`.
    - `dataModule` also provides an app-wide `CoroutineScope`. The detail ViewModel uses it to finish saving after it is cleared.
    - `NoteDetailViewModel` takes `noteId` as a Koin parameter (`koinViewModel(parameters = { parametersOf(noteId) })`) plus a `SavedStateHandle`.
  - `preferences/`: `AppSettings` and a DataStore-backed `SettingsRepository`.
    - `MainActivity` reads the settings once with `runBlocking` so the first frame already has the right theme, then collects them.
  - `presentation/UiText.kt`: string-resource wrapper used for snackbar and toast messages coming from ViewModels.
  - `ui/`: theme, transitions, modifiers, and the shared `Cute*` components.
    - `components/markdown/` holds a **handwritten Markdown parser and renderer** (`MarkdownParser`, `CuteMarkdown`); there is no library. Task blocks keep their source `lineIndex`, so tapping a checkbox in view mode can rewrite that exact line (`MarkdownParser.toggleTask`).
- `notes/`: `domain/` (models and repository interfaces), `data/` (Room entities, DAOs, mappers, `Room*Repository`), and `presentation/` (`list`, `detail`, `trash`, `shared`).
- `settings/`: the settings screen and `tags/` (Manage tags screen).

### Presentation pattern (MVI)

Each screen has:
- A `*Contract.kt`, or a contract inside the ViewModel file, defining `State`, `Action` and `Event`.
- A ViewModel that exposes `StateFlow<State>` (updated with `_state.update {}`), takes `onAction(Action)`, and sends one-off `Event`s through a `Channel`.
- A `*Screen.kt` file containing both `*Root`, which gets the ViewModel through `koinViewModel()` and handles events with NekoBites `ObserveAsEvents`, and `*Screen`, which is pure state + `onAction` and has previews.

Things that are not obvious from any single file:
- **Note detail autosave.**
  - Title and content are `TextFieldState`s owned by the ViewModel.
  - A `snapshotFlow` saves them after a 600ms debounce, behind a `Mutex`.
  - A new note is inserted on its first non-blank save. Its id goes into `SavedStateHandle`, so after process death the same note reopens.
  - `onCleared` flushes pending edits on the app scope and deletes the note if it ended up blank.
- **Trash is a soft delete:** `NoteEntity.trashedAt != null`. "Delete forever" and "Empty trash" are the only hard deletes.
- **Tags** are many-to-many through `NoteTagCrossRef`, with cascading foreign keys.
  - Tag names are unique, compared case-insensitively in the repository.
  - `renameTag` returns `false` when the name conflicts.
- **Note colors.** A note stores an optional ARGB `seedColor`.
  - `SeededTheme(seedColor)` in `core/ui/Theme.kt` wraps note cards and the whole detail screen in a MaterialKolor scheme generated from that seed.
  - The scheme follows the app's dark, AMOLED and palette-style settings, which `AzukiTheme` exposes through `LocalThemeConfig`.
  - Colored notes use the seeded `primaryContainer`; uncolored ones keep the app theme.
- **Database:** Room is at version 1 with `exportSchema = false` and no migrations, because the app is unpublished. After a schema change, clear the app's data or reinstall.

## UI conventions

The UI follows sosauce's Material 3 Expressive house style (reference app: Cinnamon). Rules that apply here:

- No top app bars. Back is a bottom-left `CuteNavigationButton`, and actions, search and toolbars float at the bottom with nav-bar and IME padding.
- Prefer NekoBites components (settings rows, `NoXFound`, `LoadingBox`, `AnimatedFab`, `bouncySpec`, `groupedShape`, `ColorPickerDialog`), then the existing `Cute*` components, then M3 Expressive.
- Use tonal `surfaceContainer*` fills, never outlines. Destructive actions use an `error` tint. Don't hardcode colors.
- Text uses the `*Emphasized` styles. Single-line text that can overflow gets `maxLines = 1` + `basicMarquee()`.
- Icons are Material Symbols Rounded XML drawables in `res/drawable` (fill `#e3e3e3`, `_filled` suffix for the filled variant), drawn with `painterResource`. Never use `Icons.*`.
- All user-facing text goes in `res/values/strings.xml`, with `plurals` for counts.
- Ask before adding dependencies.
