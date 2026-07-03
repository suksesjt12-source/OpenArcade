package off.kys.openarcade.ui.main

import off.kys.openarcade.features.settings.ui.ScreenOrientation

data class AppUiState(
    val immersiveMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val screenOrientation: ScreenOrientation = ScreenOrientation.Auto,
    val isInitialized: Boolean = false
)