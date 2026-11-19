package off.kys.openarcade.ui.main

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.transitions.SlideTransition
import off.kys.openarcade.core.ui.theme.OpenArcadeTheme
import off.kys.openarcade.features.games_launcher.ui.GamesLauncherViewModel
import off.kys.openarcade.features.astra.AstraHomeScreen
import off.kys.openarcade.features.settings.ui.ScreenOrientation
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModel()
    private val launcherViewModel: GamesLauncherViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.appState.value !is AppState.Ready || launcherViewModel.uiState.value == null
        }

        enableEdgeToEdge()
        setContent {
            val appState by viewModel.appState.collectAsStateWithLifecycle()
            val launcherUiState by launcherViewModel.uiState.collectAsStateWithLifecycle()

            if (appState is AppState.Ready && launcherUiState != null) {
                val data = (appState as AppState.Ready).data

                LaunchedEffect(data.immersiveMode) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (data.immersiveMode) {
                        windowInsetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                LaunchedEffect(data.keepScreenOn) {
                    if (data.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                LaunchedEffect(data.screenOrientation, data.forcedOrientation) {
                    if (data.forcedOrientation != null) {
                        requestedOrientation = data.forcedOrientation
                    } else {
                        requestedOrientation = when (data.screenOrientation) {
                            ScreenOrientation.Auto -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            ScreenOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            ScreenOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }
                    }
                }

                OpenArcadeTheme {
                    Navigator(AstraHomeScreen()) { navigator ->
                        if (data.reduceAnimations) {
                            FadeTransition(navigator)
                        } else {
                            SlideTransition(navigator)
                        }
                    }
                }
            }
        }
    }
}
