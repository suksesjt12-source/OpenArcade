package off.kys.openarcade.domain.repository

import kotlinx.coroutines.flow.Flow
import off.kys.openarcade.domain.model.GameEntry
import off.kys.openarcade.domain.model.SystemStatus

interface SystemRepository {
    fun fetchInstalledGames(extraPackages: List<String> = emptyList()): List<GameEntry>
    fun getSystemStatus(): Flow<SystemStatus>
    fun launchGame(packageName: String): Boolean
    fun hasUsageStatsPermission(): Boolean
}
