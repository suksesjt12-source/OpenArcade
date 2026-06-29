package off.kys.openarcade.domain.usecase

import off.kys.openarcade.domain.repository.GameRepository

class RenameGameUseCase(private val repository: GameRepository) {
    suspend operator fun invoke(packageName: String, newTitle: String?) =
        repository.updateCustomTitle(packageName, newTitle)
}
