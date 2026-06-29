package off.kys.openarcade.domain.usecase

import off.kys.openarcade.domain.repository.GameRepository

class UpdateVisibilityUseCase(private val repository: GameRepository) {
    suspend operator fun invoke(packageName: String, isHidden: Boolean) =
        repository.updateVisibility(packageName, isHidden)
}
