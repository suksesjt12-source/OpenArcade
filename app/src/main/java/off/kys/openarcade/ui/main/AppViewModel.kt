package off.kys.openarcade.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import off.kys.openarcade.core.data.local.preferences.ArcadePreferences
import off.kys.openarcade.core.domain.usecase.GetGamesUseCase
import off.kys.openarcade.core.domain.usecase.RefreshGamesUseCase
import off.kys.openarcade.features.games_launcher.domain.RefreshAllGameStatsUseCase

class AppViewModel(
    prefs: ArcadePreferences,
    getGamesUseCase: GetGamesUseCase,
    private val refreshGamesUseCase: RefreshGamesUseCase,
    private val refreshAllGameStatsUseCase: RefreshAllGameStatsUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(true)

    val uiState: StateFlow<AppUiState> = combine(
        prefs.immersiveMode,
        prefs.keepScreenOn,
        prefs.screenOrientation,
        getGamesUseCase(),
        _isRefreshing
    ) { immersive, keepOn, orientation, games, refreshing ->
        AppUiState(
            immersiveMode = immersive,
            keepScreenOn = keepOn,
            screenOrientation = orientation,
            isInitialized = !refreshing || games.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState()
    )

    init {
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshGamesUseCase()
                refreshAllGameStatsUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }
}
