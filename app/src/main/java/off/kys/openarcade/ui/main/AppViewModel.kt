package off.kys.openarcade.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import off.kys.openarcade.core.data.local.preferences.ArcadePreferences
import off.kys.openarcade.core.domain.usecase.GetGamesUseCase
import off.kys.openarcade.core.domain.usecase.GetSystemStatusUseCase
import off.kys.openarcade.core.domain.usecase.RefreshGamesUseCase
import off.kys.openarcade.features.games_launcher.domain.RefreshAllGameStatsUseCase

class AppViewModel(
    prefs: ArcadePreferences,
    getGamesUseCase: GetGamesUseCase,
    getSystemStatusUseCase: GetSystemStatusUseCase,
    private val refreshGamesUseCase: RefreshGamesUseCase,
    private val refreshAllGameStatsUseCase: RefreshAllGameStatsUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(true)

    val appState: StateFlow<AppState> = combine(
        combine(
            getGamesUseCase(),
            getSystemStatusUseCase(),
            prefs.immersiveMode,
            prefs.keepScreenOn
        ) { games, status, immersive, keepOn ->
            Quad(games, status, immersive, keepOn)
        },
        prefs.screenOrientation,
        _isRefreshing
    ) { quad, orientation, refreshing ->
        AppStateData(
            data = GameCoreData(
                games = quad.first,
                systemStatus = quad.second,
                immersiveMode = quad.third,
                keepScreenOn = quad.fourth,
                screenOrientation = orientation
            ),
            isRefreshing = refreshing,
            hasGames = quad.first.isNotEmpty()
        )
    }.map { stateData ->
        if (stateData.isRefreshing && !stateData.hasGames) {
            AppState.Loading
        } else {
            AppState.Ready(stateData.data)
        }
    }.onStart {
        emit(AppState.Loading)
    }.catch { e ->
        emit(AppState.Error(e.message ?: "Unknown Error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppState.Loading
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

    private data class AppStateData(
        val data: GameCoreData,
        val isRefreshing: Boolean,
        val hasGames: Boolean
    )

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
