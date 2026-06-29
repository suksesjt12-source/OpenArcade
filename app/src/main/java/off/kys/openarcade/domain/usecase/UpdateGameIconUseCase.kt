package off.kys.openarcade.domain.usecase

import off.kys.openarcade.domain.repository.GameRepository

class UpdateGameIconUseCase(private val repository: GameRepository) {
    suspend operator fun invoke(packageName: String, newIconPath: String?) =
        repository.updateCustomIconPath(packageName, newIconPath)
}
