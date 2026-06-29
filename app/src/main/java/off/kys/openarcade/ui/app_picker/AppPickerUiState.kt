package off.kys.openarcade.ui.app_picker

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import off.kys.openarcade.domain.model.AppInfo

data class AppPickerUiState(
    val apps: ImmutableList<AppInfo> = persistentListOf(),
    val trackedPackages: ImmutableMap<String, Boolean> = persistentMapOf(), // packageName to isHidden
    val searchQuery: String = "",
    val selectedPackages: ImmutableSet<String> = persistentSetOf(),
    val isLoading: Boolean = false,
    val isDone: Boolean = false,
    val showScrollbar: Boolean = true,
    val hapticFeedback: Boolean = true
) {
    val filteredApps: ImmutableList<AppInfo>
        get() = apps.filter { app ->
            val isTracked = trackedPackages.containsKey(app.packageName)
            val isHidden = trackedPackages[app.packageName] ?: false

            // Show if not tracked OR if tracked but hidden (excluded)
            val shouldShow = !isTracked || isHidden

            shouldShow && (
                    app.label.contains(searchQuery, ignoreCase = true) ||
                            app.packageName.contains(searchQuery, ignoreCase = true)
                    )
        }.toPersistentList()
}
