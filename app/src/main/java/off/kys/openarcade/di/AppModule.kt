package off.kys.openarcade.di

import androidx.room.Room
import off.kys.openarcade.data.local.AppDatabase
import off.kys.openarcade.data.local.ArcadePreferences
import off.kys.openarcade.data.repository.GameRepositoryImpl
import off.kys.openarcade.data.repository.MediaRepositoryImpl
import off.kys.openarcade.data.repository.SystemRepositoryImpl
import off.kys.openarcade.domain.repository.GameRepository
import off.kys.openarcade.domain.repository.MediaRepository
import off.kys.openarcade.domain.repository.SystemRepository
import off.kys.openarcade.domain.usecase.*
import off.kys.openarcade.ui.analytics.AnalyticsViewModel
import off.kys.openarcade.ui.app_picker.AppPickerViewModel
import off.kys.openarcade.ui.detail.GameDetailViewModel
import off.kys.openarcade.ui.launcher.GamesLauncherViewModel
import off.kys.openarcade.ui.main.AppViewModel
import off.kys.openarcade.ui.settings.SettingsViewModel
import off.kys.openarcade.util.*
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
