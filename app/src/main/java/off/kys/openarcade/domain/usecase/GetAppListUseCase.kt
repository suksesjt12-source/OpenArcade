package off.kys.openarcade.domain.usecase

import android.app.Application
import android.content.Intent
import off.kys.openarcade.domain.model.AppInfo

class GetAppListUseCase(private val application: Application) {
    operator fun invoke(): List<AppInfo> {
        val pm = application.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
