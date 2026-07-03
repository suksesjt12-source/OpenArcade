package off.kys.openarcade.di

import androidx.room.Room
import off.kys.openarcade.core.data.local.database.AppDatabase
import off.kys.openarcade.core.data.local.preferences.ArcadePreferences
import off.kys.openarcade.core.data.repository.GameRepositoryImpl
import off.kys.openarcade.core.data.repository.MediaRepositoryImpl
import off.kys.openarcade.core.data.repository.SystemRepositoryImpl
import off.kys.openarcade.core.domain.repository.GameRepository
import off.kys.openarcade.core.domain.repository.MediaRepository
import off.kys.openarcade.core.domain.repository.SystemRepository
import off.kys.openarcade.core.domain.usecase.*
import off.kys.openarcade.features.analytics.domain.GetAnalyticsDataUseCase
import off.kys.openarcade.features.app_picker.domain.GetAppListUseCase
import off.kys.openarcade.features.game_details.domain.GetGameByPackageUseCase
import off.kys.openarcade.features.game_details.domain.LaunchGameUseCase
import off.kys.openarcade.features.game_details.domain.RefreshGameStatsUseCase
import off.kys.openarcade.features.games_launcher.domain.*
import off.kys.openarcade.features.analytics.ui.AnalyticsViewModel
import off.kys.openarcade.features.app_picker.ui.AppPickerViewModel
import off.kys.openarcade.features.game_details.ui.GameDetailViewModel
import off.kys.openarcade.features.games_launcher.ui.GamesLauncherViewModel
import off.kys.openarcade.ui.main.AppViewModel
import off.kys.openarcade.features.settings.ui.SettingsViewModel
import off.kys.openarcade.core.util.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java, "openarcade-db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { get<AppDatabase>().gameDao() }

    single { ArcadePreferences(androidContext()) }

    single { IconManager(androidContext()) }
    single { ColorExtractor() }
    single { GameScanner(androidContext()) }
    single { TimeUtils(androidContext()) }

    single<MediaRepository> { MediaRepositoryImpl(get(), get()) }
    single<SystemRepository> { SystemRepositoryImpl(androidApplication(), get()) }

    single<GameRepository> { GameRepositoryImpl(androidContext(), get(), get(), get()) }

    factoryOf(::GetGamesUseCase)
    factoryOf(::RefreshGamesUseCase)
    factoryOf(::RefreshGameStatsUseCase)
    factoryOf(::RefreshAllGameStatsUseCase)
    factoryOf(::GetGameByPackageUseCase)
    factoryOf(::UpdateGameCategoryUseCase)
    factoryOf(::GetAnalyticsDataUseCase)
    factoryOf(::GetSystemStatusUseCase)
    factoryOf(::LaunchGameUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    factoryOf(::UpdateVisibilityUseCase)
    factoryOf(::RenameGameUseCase)
    factoryOf(::UpdateGameIconUseCase)
    factoryOf(::AddGamesUseCase)
    factoryOf(::GetAppListUseCase)

    viewModelOf(::AppViewModel)
    viewModelOf(::GamesLauncherViewModel)
    viewModelOf(::AppPickerViewModel)
    viewModelOf(::AnalyticsViewModel)
    viewModelOf(::SettingsViewModel)

    viewModel { (packageName: String) ->
        GameDetailViewModel(
            packageName = packageName,
            getGameByPackageUseCase = get(),
            updateGameCategoryUseCase = get(),
            refreshGameStatsUseCase = get(),
            launchGameUseCase = get(),
            prefs = get()
        )
    }
}
