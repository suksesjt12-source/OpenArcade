package off.kys.openarcade.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import off.kys.openarcade.data.local.ArcadePreferences
import off.kys.openarcade.domain.model.GameCategory
import off.kys.openarcade.domain.model.GameEntry
import off.kys.openarcade.domain.model.GameFilter
import off.kys.openarcade.domain.model.GameSortOption
import off.kys.openarcade.domain.usecase.AddGamesUseCase
import off.kys.openarcade.domain.usecase.GetGamesUseCase
import off.kys.openarcade.domain.usecase.GetSystemStatusUseCase
import off.kys.openarcade.domain.usecase.RefreshAllGameStatsUseCase
import off.kys.openarcade.domain.usecase.RefreshGamesUseCase
import off.kys.openarcade.domain.usecase.RenameGameUseCase
import off.kys.openarcade.domain.usecase.ToggleFavoriteUseCase
import off.kys.openarcade.domain.usecase.UpdateGameCategoryUseCase
import off.kys.openarcade.domain.usecase.UpdateGameIconUseCase
import off.kys.openarcade.domain.usecase.UpdateVisibilityUseCase
import off.kys.openarcade.ui.settings.GridColumns
import off.kys.openarcade.ui.settings.ScreenOrientation

class GamesLauncherViewModel(
    private val refreshGamesUseCase: RefreshGamesUseCase,
    private val refreshAllGameStatsUseCase: RefreshAllGameStatsUseCase,
    getGamesUseCase: GetGamesUseCase,
    getSystemStatusUseCase: GetSystemStatusUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateVisibilityUseCase: UpdateVisibilityUseCase,
    private val renameGameUseCase: RenameGameUseCase,
    private val updateGameIconUseCase: UpdateGameIconUseCase,
    private val addGamesUseCase: AddGamesUseCase,
    private val updateGameCategoryUseCase: UpdateGameCategoryUseCase,
    private val prefs: ArcadePreferences
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<GameFilter>(GameFilter.All)
    private val isLoading = MutableStateFlow(true)
    private val _tagState = MutableStateFlow(TagDialogState())

    private val allGames: StateFlow<List<GameEntry>> = getGamesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val systemStatus = getSystemStatusUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = off.kys.openarcade.domain.model.SystemStatus()
        )

    private val availableFilters: StateFlow<List<GameFilter>> = allGames.map { games ->
        val filters = mutableListOf<GameFilter>()
        if (games.isNotEmpty()) {
            filters.add(GameFilter.All)
            filters.add(GameFilter.Installed)
            if (games.any { !it.isInstalled }) {
                filters.add(GameFilter.Uninstalled)
            }
            if (games.any { it.isHidden }) {
                filters.add(GameFilter.Hidden)
            }
        }

        GameCategory.entries.forEach { category ->
            if (category != GameCategory.UNDEFINED && games.any { it.category == category }) {
                filters.add(GameFilter.System(category))
            }
        }

        val custom = games.flatMap { it.customCategories }.distinct().sorted()
        custom.forEach { filters.add(GameFilter.Custom(it)) }

        filters
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(GameFilter.All)
        )

    private val combinedGamesData = combine(
        allGames,
        availableFilters,
        selectedFilter
    ) { all, filters, selected ->
        GamesDataState(all, filters, selected)
    }

    private val preferenceState = combine(
        combine(
            prefs.selectedSort,
            prefs.visibleSections,
            prefs.gridColumns,
            prefs.screenOrientation
        ) { sort, sections, grid, orient ->
            val gridCols = GridColumns.entries.firstOrNull { it == grid }
                ?: GridColumns.Three
            val orientation = ScreenOrientation.entries.firstOrNull { it == orient }
                ?: ScreenOrientation.Auto
            PreferenceStatePart1(sort, sections, gridCols, orientation)
        },
        combine(
            prefs.immersiveMode,
            prefs.keepScreenOn,
            prefs.showScrollbar,
            prefs.hapticFeedback
        ) { immersive, keep, scroll, haptic ->
            PreferenceStatePart2(immersive, keep, scroll, haptic)
        }
    ) { p1, p2 ->
        PreferenceState(
            sort = p1.sort,
            sections = p1.sections,
            gridColumns = p1.grid,
            orientation = p1.orient,
            immersiveMode = p2.immersive,
            keepScreenOn = p2.keep,
            showScrollbar = p2.scroll,
            hapticFeedback = p2.haptic
        )
    }

    private val combinedTransientState = combine(
        isLoading,
        _tagState
    ) { loading, tags ->
        TransientState(loading, tags)
    }

    val uiState: StateFlow<GamesLauncherUiState> = combine(
        combinedGamesData,
        systemStatus,
        preferenceState,
        combinedTransientState
    ) { games, status, prefs, transient ->
        val favorites = mutableListOf<GameEntry>()
        val recent = mutableListOf<GameEntry>()
        val filtered = mutableListOf<GameEntry>()

        games.all.forEach { game ->
            if (game.isFavorite) favorites.add(game)
            if (game.lastPlayed > 0) recent.add(game)

            val matches = when (games.selected) {
                is GameFilter.All -> !game.isHidden
                is GameFilter.Installed -> !game.isHidden && game.isInstalled
                is GameFilter.Uninstalled -> !game.isHidden && !game.isInstalled
                is GameFilter.System -> !game.isHidden && game.category == games.selected.category
                is GameFilter.Custom -> !game.isHidden && games.selected.name in game.customCategories
                is GameFilter.Hidden -> game.isHidden
            }
            if (matches) filtered.add(game)
        }

        val sortedFiltered = when (prefs.sort) {
            GameSortOption.TITLE_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            GameSortOption.TITLE_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            GameSortOption.LAST_PLAYED -> filtered.sortedByDescending { it.lastPlayed }
            GameSortOption.TOTAL_PLAY_TIME -> filtered.sortedByDescending { it.totalPlayTime }
        }

        GamesLauncherUiState(
            filteredGames = sortedFiltered.toPersistentList(),
            favoriteGames = favorites.sortedBy { it.displayName.lowercase() }.toPersistentList(),
            recentGames = recent.sortedByDescending { it.lastPlayed }.take(5).toPersistentList(),
            filters = games.filters.toPersistentList(),
            selectedFilter = games.selected,
            selectedSort = prefs.sort,
            visibleSections = prefs.sections.toPersistentSet(),
            gridColumns = prefs.gridColumns,
            screenOrientation = prefs.orientation,
            immersiveMode = prefs.immersiveMode,
            keepScreenOn = prefs.keepScreenOn,
            showScrollbar = prefs.showScrollbar,
            hapticFeedback = prefs.hapticFeedback,
            batteryLevel = status.batteryLevel,
            storageUsage = status.storageUsagePercentage,
            hasUsageStatsPermission = status.hasUsageStatsPermission,
            isLoading = transient.isLoading,
            showTagDialog = transient.tagState.showTagDialog,
            taggingGame = transient.tagState.taggingGame,
            editingTags = transient.tagState.editingTags,
            newTagDraft = transient.tagState.newTagDraft
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GamesLauncherUiState()
        )

    init {
        refreshGames()
    }

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
        is GamesLauncherUiEvent.GrantPermissionClicked -> { /* Handled by UI */ }
        is GamesLauncherUiEvent.PermissionCheckRequested -> { /* Handled by status flow */ }
        is GamesLauncherUiEvent.RefreshStats -> refreshStats()

        is GamesLauncherUiEvent.FavoriteToggled -> {
            viewModelScope.launch {
                toggleFavoriteUseCase(event.packageName, event.isFavorite)
            }
        }

        is GamesLauncherUiEvent.VisibilityToggled -> {
            viewModelScope.launch {
                updateVisibilityUseCase(event.packageName, event.isHidden)
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
            isLoading.value = true
            try {
                refreshGamesUseCase()
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            refreshAllGameStatsUseCase()
        }
    }
}