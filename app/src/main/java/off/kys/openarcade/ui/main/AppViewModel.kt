package off.kys.openarcade.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import off.kys.openarcade.data.local.ArcadePreferences
import off.kys.openarcade.domain.usecase.GetGamesUseCase
import off.kys.openarcade.ui.settings.ScreenOrientation

data class AppUiState(
    val immersiveMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Auto,
    val isInitialized: Boolean = false
)

class AppViewModel(
    private val prefs: ArcadePreferences,
    getGamesUseCase: GetGamesUseCase
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = combine(
        prefs.immersiveMode,
        prefs.keepScreenOn,
        prefs.screenOrientation,
        getGamesUseCase()
    ) { immersive, keepOn, orientation, games ->
        AppUiState(
            immersiveMode = immersive,
            keepScreenOn = keepOn,
            screenOrientation = orientation,
            isInitialized = games.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState()
    )
}
