package off.kys.openarcade.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val packageName: String,
    val label: String
)
