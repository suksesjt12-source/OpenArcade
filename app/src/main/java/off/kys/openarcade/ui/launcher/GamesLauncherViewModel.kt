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
import kotlinx.coroutines.launch
import off.kys.openarcade.data.local.ArcadePreferences
import off.kys.openarcade.domain.model.GameCategory
import off.kys.openarcade.domain.model.GameEntry
import off.kys.openarcade.domain.model.GameFilter
import off.kys.openarcade.domain.model.GameSortOption
import off.kys.openarcade.domain.model.LauncherSection
import off.kys.openarcade.domain.usecase.*

class GamesLauncherViewModel(
    private val refreshGamesUseCase: RefreshGamesUseCase,
    private val refreshAllGameStatsUseCase: RefreshAllGameStatsUseCase,
    private val getGamesUseCase: GetGamesUseCase,
    private val getSystemStatusUseCase: GetSystemStatusUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val updateVisibilityUseCase: UpdateVisibilityUseCase,
    private val renameGameUseCase: RenameGameUseCase,
    private val updateGameIconUseCase: UpdateGameIconUseCase,
    private val addGamesUseCase: AddGamesUseCase,
    private val prefs: ArcadePreferences
) : ViewModel() {

    private val selectedFilter = MutableStateFlow<GameFilter>(GameFilter.All)
    private val isLoading = MutableStateFlow(true)

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

    val uiState: StateFlow<GamesLauncherUiState> = combine(
        allGames,
        availableFilters,
        selectedFilter,
        systemStatus,
        prefs.selectedSort,
        prefs.visibleSections,
        prefs.gridColumns,
        prefs.screenOrientation,
        prefs.immersiveMode,
        prefs.keepScreenOn,
        prefs.showScrollbar,
        prefs.hapticFeedback,
        isLoading
    ) { params ->
        val all = params[0] as List<GameEntry>
        val filters = params[1] as List<GameFilter>
        val selected = params[2] as GameFilter
        val status = params[3] as off.kys.openarcade.domain.model.SystemStatus
        val sort = params[4] as GameSortOption
        val sections = params[5] as Set<LauncherSection>
        val gridCols = off.kys.openarcade.ui.settings.GridColumns.entries.firstOrNull { it == params[6] } ?: off.kys.openarcade.ui.settings.GridColumns.Three
        val orientation = off.kys.openarcade.ui.settings.ScreenOrientation.entries.firstOrNull { it == params[7] } ?: off.kys.openarcade.ui.settings.ScreenOrientation.Auto
        val immersive = params[8] as Boolean
        val keepOn = params[9] as Boolean
        val scrollbar = params[10] as Boolean
        val haptic = params[11] as Boolean
        val loading = params[12] as Boolean

        val visibleGames = all.filter { !it.isHidden }

        val filtered = when (selected) {
            is GameFilter.All -> visibleGames
            is GameFilter.Installed -> visibleGames.filter { it.isInstalled }
            is GameFilter.Uninstalled -> visibleGames.filter { !it.isInstalled }
            is GameFilter.System -> visibleGames.filter { it.category == selected.category }
            is GameFilter.Custom -> visibleGames.filter { selected.name in it.customCategories }
            is GameFilter.Hidden -> all.filter { it.isHidden }
        }

        val sorted = when (sort) {
            GameSortOption.TITLE_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            GameSortOption.TITLE_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            GameSortOption.LAST_PLAYED -> filtered.sortedByDescending { it.lastPlayed }
            GameSortOption.TOTAL_PLAY_TIME -> filtered.sortedByDescending { it.totalPlayTime }
        }

        val favorites = all.filter { it.isFavorite }
            .sortedBy { it.displayName.lowercase() }

        val recent = all.filter { it.lastPlayed > 0 }
            .sortedByDescending { it.lastPlayed }
            .take(5)

        GamesLauncherUiState(
            filteredGames = sorted.toPersistentList(),
            favoriteGames = favorites.toPersistentList(),
            recentGames = recent.toPersistentList(),
            filters = filters.toPersistentList(),
            selectedFilter = selected,
            selectedSort = sort,
            visibleSections = sections.toPersistentSet(),
            gridColumns = gridCols,
            screenOrientation = orientation,
            immersiveMode = immersive,
            keepScreenOn = keepOn,
            showScrollbar = scrollbar,
            hapticFeedback = haptic,
            batteryLevel = status.batteryLevel,
            storageUsage = status.storageUsagePercentage,
            hasUsageStatsPermission = status.hasUsageStatsPermission,
            isLoading = loading
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
