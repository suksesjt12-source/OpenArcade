package off.kys.openarcade.features.games_launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import off.kys.openarcade.core.data.local.preferences.ArcadePreferences
import off.kys.openarcade.core.domain.model.GameCategory
import off.kys.openarcade.core.domain.model.GameEntry
import off.kys.openarcade.core.domain.model.GameFilter
import off.kys.openarcade.core.domain.model.GameSortOption
import off.kys.openarcade.core.domain.usecase.AddGamesUseCase
import off.kys.openarcade.core.domain.usecase.RefreshGamesUseCase
import off.kys.openarcade.core.domain.usecase.RemoveGameUseCase
import off.kys.openarcade.features.game_details.domain.LaunchGameUseCase
import off.kys.openarcade.features.games_launcher.domain.RefreshAllGameStatsUseCase
import off.kys.openarcade.features.games_launcher.domain.RenameGameUseCase
import off.kys.openarcade.features.games_launcher.domain.ToggleFavoriteUseCase
import off.kys.openarcade.features.games_launcher.domain.UpdateGameCategoryUseCase
import off.kys.openarcade.features.games_launcher.domain.UpdateGameIconUseCase
import off.kys.openarcade.ui.main.AppState
import off.kys.openarcade.ui.main.AppViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class GamesLauncherViewModel(
    private val refreshGamesUseCase: RefreshGamesUseCase,
    private val refreshAllGameStatsUseCase: RefreshAllGameStatsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val removeGameUseCase: RemoveGameUseCase,
    private val renameGameUseCase: RenameGameUseCase,
    private val updateGameIconUseCase: UpdateGameIconUseCase,
    private val addGamesUseCase: AddGamesUseCase,
    private val updateGameCategoryUseCase: UpdateGameCategoryUseCase,
    private val launchGameUseCase: LaunchGameUseCase,
    private val prefs: ArcadePreferences,
    appViewModel: AppViewModel
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<GameFilter>(GameFilter.All)
    private val _tagState = MutableStateFlow(TagDialogState())

    val uiState: StateFlow<GamesLauncherUiState?> = appViewModel.appState
        .filterIsInstance<AppState.Ready>()
        .flatMapLatest { ready ->
            val data = ready.data
            combine(
                selectedFilter,
                _tagState,
                prefs.selectedSort,
                prefs.visibleSections,
                combine(
                    prefs.gridColumns,
                    prefs.showScrollbar,
                    prefs.hapticFeedback,
                    prefs.skipGameDetails
                ) { grid, scroll, haptic, skipDetails ->
                    Quadruple(grid, scroll, haptic, skipDetails)
                }
            ) { filter, tags, sort, sections, extra ->
                val (grid, scroll, haptic, skipDetails) = extra
                val allGames = data.games
                val status = data.systemStatus

                val availableFilters = mutableListOf<GameFilter>()
                if (allGames.isNotEmpty()) {
                    availableFilters.add(GameFilter.All)
                    availableFilters.add(GameFilter.Installed)
                    if (allGames.any { !it.isInstalled }) {
                        availableFilters.add(GameFilter.Uninstalled)
                    }
                }
                GameCategory.entries.forEach { category ->
                    if (category != GameCategory.UNDEFINED && allGames.any { it.category == category }) {
                        availableFilters.add(GameFilter.System(category))
                    }
                }
                val custom = allGames.flatMap { it.customCategories }.distinct().sorted()
                custom.forEach { availableFilters.add(GameFilter.Custom(it)) }

                val favorites = mutableListOf<GameEntry>()
                val recent = mutableListOf<GameEntry>()
                val filtered = mutableListOf<GameEntry>()

                allGames.forEach { game ->
                    if (game.isFavorite) favorites.add(game)
                    if (game.lastPlayed > 0) recent.add(game)

                    val matches = when (filter) {
                        is GameFilter.All -> true
                        is GameFilter.Installed -> game.isInstalled
                        is GameFilter.Uninstalled -> !game.isInstalled
                        is GameFilter.System -> game.category == filter.category
                        is GameFilter.Custom -> filter.name in game.customCategories
                    }
                    if (matches) filtered.add(game)
                }

                val sortedFiltered = when (sort) {
                    GameSortOption.TITLE_ASC -> filtered.sortedBy { it.displayName.lowercase() }
                    GameSortOption.TITLE_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
                    GameSortOption.LAST_PLAYED -> filtered.sortedByDescending { it.lastPlayed }
                    GameSortOption.TOTAL_PLAY_TIME -> filtered.sortedByDescending { it.totalPlayTime }
                }

                GamesLauncherUiState(
                    filteredGames = sortedFiltered.toPersistentList(),
                    favoriteGames = favorites.sortedBy { it.displayName.lowercase() }.toPersistentList(),
                    recentGames = recent.sortedByDescending { it.lastPlayed }.take(5).toPersistentList(),
                    filters = availableFilters.toPersistentList(),
                    selectedFilter = filter,
                    selectedSort = sort,
                    visibleSections = sections.toPersistentSet(),
                    gridColumns = grid,
                    screenOrientation = data.screenOrientation,
                    immersiveMode = data.immersiveMode,
                    keepScreenOn = data.keepScreenOn,
                    showScrollbar = scroll,
                    hapticFeedback = haptic,
                    skipGameDetails = skipDetails,
                    batteryLevel = status.batteryLevel,
                    storageUsage = status.storageUsagePercentage,
                    hasUsageStatsPermission = status.hasUsageStatsPermission,
                    showTagDialog = tags.showTagDialog,
                    taggingGame = tags.taggingGame,
                    editingTags = tags.editingTags,
                    newTagDraft = tags.newTagDraft
                )
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun onEvent(event: GamesLauncherUiEvent) = when (event) {
        is GamesLauncherUiEvent.FilterSelected -> {
            selectedFilter.value = event.filter
        }

        is GamesLauncherUiEvent.SortSelected -> {
            prefs.setSortOption(event.sort)
        }

        is GamesLauncherUiEvent.SectionVisibilityToggled -> {
            prefs.setSectionVisible(event.section, event.isVisible)
        }

        is GamesLauncherUiEvent.RefreshRequested -> refreshGames()
        is GamesLauncherUiEvent.GameClicked -> { /* Handled by UI */ }
        is GamesLauncherUiEvent.LaunchGameRequested -> {
            launchGameUseCase(event.packageName)
        }
        is GamesLauncherUiEvent.GrantPermissionClicked -> { /* Handled by UI */ }
        is GamesLauncherUiEvent.PermissionCheckRequested -> { /* Handled by status flow in AppViewModel */ }
        is GamesLauncherUiEvent.RefreshStats -> refreshStats()

        is GamesLauncherUiEvent.FavoriteToggled -> {
            viewModelScope.launch {
                toggleFavoriteUseCase(event.packageName, event.isFavorite)
            }
        }

        is GamesLauncherUiEvent.RemoveGame -> {
            viewModelScope.launch {
                removeGameUseCase(event.packageName)
            }
        }

        is GamesLauncherUiEvent.RenameRequested -> {
            viewModelScope.launch {
                renameGameUseCase(event.packageName, event.newTitle?.takeIf { it.isNotBlank() })
            }
        }

        is GamesLauncherUiEvent.ChangeIconRequested -> {
            viewModelScope.launch {
                updateGameIconUseCase(event.packageName, event.newIconPath)
            }
        }

        is GamesLauncherUiEvent.AppInfoRequested -> { /* Handled by UI */ }
        is GamesLauncherUiEvent.UninstallRequested -> { /* Handled by UI */ }

        is GamesLauncherUiEvent.AddGamesRequested -> {
            viewModelScope.launch {
                addGamesUseCase(event.packageNames)
                refreshGames()
            }
        }

        is GamesLauncherUiEvent.ManageTagsRequested -> {
            _tagState.update {
                it.copy(
                    showTagDialog = true,
                    taggingGame = event.game,
                    editingTags = event.game.customCategories,
                    newTagDraft = ""
                )
            }
        }

        is GamesLauncherUiEvent.TagDialogDismissed -> {
            _tagState.update { it.copy(showTagDialog = false) }
        }

        is GamesLauncherUiEvent.TagAdded -> {
            _tagState.update { state ->
                val trimmed = event.tag.trim()
                if (trimmed.isNotBlank() && trimmed !in state.editingTags) {
                    state.copy(
                        editingTags = (state.editingTags + trimmed).toPersistentList(),
                        newTagDraft = ""
                    )
                } else state
            }
        }

        is GamesLauncherUiEvent.TagRemoved -> {
            _tagState.update { state ->
                state.copy(
                    editingTags = (state.editingTags - event.tag).toPersistentList()
                )
            }
        }

        is GamesLauncherUiEvent.TagDraftChanged -> {
            _tagState.update { it.copy(newTagDraft = event.text) }
        }

        is GamesLauncherUiEvent.TagsSaved -> {
            val game = _tagState.value.taggingGame
            if (game != null) {
                viewModelScope.launch {
                    updateGameCategoryUseCase(game.packageName, _tagState.value.editingTags)
                    _tagState.update { it.copy(showTagDialog = false) }
                }
            } else {
                _tagState.update { it.copy(showTagDialog = false) }
            }
        }
    }

    private fun refreshGames() {
        viewModelScope.launch {
            try {
                refreshGamesUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            refreshAllGameStatsUseCase()
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
