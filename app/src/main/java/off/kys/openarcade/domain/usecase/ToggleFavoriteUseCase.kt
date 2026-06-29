package off.kys.openarcade.domain.usecase

import off.kys.openarcade.domain.repository.GameRepository

class ToggleFavoriteUseCase(private val repository: GameRepository) {
    suspend operator fun invoke(packageName: String, isFavorite: Boolean) =
        repository.updateFavoriteStatus(packageName, isFavorite)
}
