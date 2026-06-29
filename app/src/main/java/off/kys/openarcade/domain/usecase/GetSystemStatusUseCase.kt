package off.kys.openarcade.domain.usecase

import off.kys.openarcade.domain.repository.SystemRepository

class GetSystemStatusUseCase(private val repository: SystemRepository) {
    operator fun invoke() = repository.getSystemStatus()
}
