package off.kys.openarcade.ui.launcher

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentSet
import off.kys.openarcade.domain.model.GameEntry
import off.kys.openarcade.domain.model.GameFilter
import off.kys.openarcade.domain.model.GameSortOption
import off.kys.openarcade.domain.model.LauncherSection
import off.kys.openarcade.ui.settings.GridColumns
import off.kys.openarcade.ui.settings.ScreenOrientation

data class GamesLauncherUiState(
    val filteredGames: ImmutableList<GameEntry> = persistentListOf(),
    val favoriteGames: ImmutableList<GameEntry> = persistentListOf(),
    val recentGames: ImmutableList<GameEntry> = persistentListOf(),
    val filters: ImmutableList<GameFilter> = persistentListOf(GameFilter.All),
    val selectedFilter: GameFilter = GameFilter.All,
    val selectedSort: GameSortOption = GameSortOption.TITLE_ASC,
    val visibleSections: ImmutableSet<LauncherSection> = LauncherSection.entries.toPersistentSet(),
    val gridColumns: GridColumns = GridColumns.Three,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Auto,
    val immersiveMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showScrollbar: Boolean = true,
    val hapticFeedback: Boolean = true,
    val batteryLevel: Int = 0,
    val storageUsage: Int = 0,
    val hasUsageStatsPermission: Boolean = true,
    val isLoading: Boolean = true,
    val showTagDialog: Boolean = false,
    val taggingGame: GameEntry? = null,
    val editingTags: ImmutableList<String> = persistentListOf(),
    val newTagDraft: String = ""
)

data class TagDialogState(
    val showTagDialog: Boolean = false,
    val taggingGame: GameEntry? = null,
    val editingTags: ImmutableList<String> = persistentListOf(),
    val newTagDraft: String = ""
)

data class GamesDataState(
    val all: List<GameEntry>,
    val filters: List<GameFilter>,
    val selected: GameFilter
)

data class PreferenceState(
    val sort: GameSortOption,
    val sections: Set<LauncherSection>,
    val gridColumns: GridColumns,
    val orientation: ScreenOrientation,
    val immersiveMode: Boolean,
    val keepScreenOn: Boolean,
    val showScrollbar: Boolean,
    val hapticFeedback: Boolean
)

data class PreferenceStatePart1(
    val sort: GameSortOption,
    val sections: Set<LauncherSection>,
    val grid: GridColumns,
    val orient: ScreenOrientation
)

data class PreferenceStatePart2(
    val immersive: Boolean,
    val keep: Boolean,
    val scroll: Boolean,
    val haptic: Boolean
)

data class TransientState(
    val isLoading: Boolean,
    val tagState: TagDialogState
)