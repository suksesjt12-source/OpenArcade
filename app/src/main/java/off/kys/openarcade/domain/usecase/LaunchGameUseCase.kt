package off.kys.openarcade.domain.usecase

import off.kys.openarcade.domain.repository.SystemRepository

class LaunchGameUseCase(private val repository: SystemRepository) {
    operator fun invoke(packageName: String) = repository.launchGame(packageName)
}
