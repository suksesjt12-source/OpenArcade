package off.kys.openarcade.features.astra.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns

/**
 * External Winlator integration.
 *
 * AstraNS does not bundle Wine/Box64. It discovers an installed Winlator build and
 * can open it, or forward an exported .desktop shortcut to compatible front-end
 * integrations that consume the "frontend_desktop_path" intent extra.
 */
data class WindowsRuntime(
    val packageName: String,
    val label: String
)

data class RuntimeLaunchResult(
    val launched: Boolean,
    val message: String
)

object RuntimeIntegration {
    const val FRONTEND_DESKTOP_PATH_EXTRA = "frontend_desktop_path"

    fun findInstalledWinlatorBuilds(context: Context): List<WindowsRuntime> {
        val packageManager = context.packageManager
        val applications: List<ApplicationInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
        } catch (_: Exception) {
            emptyList()
        }

        return applications.mapNotNull { application ->
            val label = try {
                packageManager.getApplicationLabel(application).toString()
            } catch (_: Exception) {
                application.packageName
            }
            if (label.contains("winlator", ignoreCase = true) ||
                application.packageName.contains("winlator", ignoreCase = true)
            ) {
                WindowsRuntime(
                    packageName = application.packageName,
                    label = label
                )
            } else {
                null
            }
        }.filter { runtime ->
            packageManager.getLaunchIntentForPackage(runtime.packageName) != null
        }.distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun openRuntime(context: Context, packageName: String): RuntimeLaunchResult {
        if (packageName.isBlank()) {
            return RuntimeLaunchResult(false, "Pilih runtime Winlator terlebih dahulu.")
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return RuntimeLaunchResult(false, "Aplikasi runtime tidak memiliki aktivitas peluncuran.")

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            RuntimeLaunchResult(true, "Runtime dibuka. Pilih container dan game dari Winlator.")
        } catch (_: Exception) {
            RuntimeLaunchResult(false, "Runtime gagal dibuka. Periksa apakah aplikasinya masih terpasang.")
        }
    }

    fun launchDesktopShortcut(
        context: Context,
        packageName: String,
        desktopPath: String
    ): RuntimeLaunchResult {
        val path = desktopPath.trim()
        if (packageName.isBlank()) {
            return RuntimeLaunchResult(false, "Pilih runtime Winlator terlebih dahulu.")
        }
        if (!path.startsWith("/") || !path.endsWith(".desktop", ignoreCase = true)) {
            return RuntimeLaunchResult(
                false,
                "Masukkan path absolut ke file shortcut .desktop yang diekspor Winlator."
            )
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return RuntimeLaunchResult(false, "Runtime yang dipilih tidak dapat dibuka.")

        return try {
            launchIntent.putExtra(FRONTEND_DESKTOP_PATH_EXTRA, path)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(launchIntent)
            RuntimeLaunchResult(
                true,
                "Permintaan shortcut dikirim. Auto-launch hanya bekerja pada build Winlator yang mendukung frontend_desktop_path; build standar mungkin hanya membuka layar utama Winlator."
            )
        } catch (_: Exception) {
            RuntimeLaunchResult(false, "Shortcut gagal dikirim ke runtime.")
        }
    }

    /**
     * Resolve common Android document-picker URIs to a filesystem path.
     * Some providers (for example cloud drives) intentionally do not expose one.
     */
    fun resolveFilesystemPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path

        return try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                when (uri.authority) {
                    "com.android.externalstorage.documents" -> {
                        val documentId = DocumentsContract.getDocumentId(uri)
                        val split = documentId.split(":", limit = 2)
                        if (split.size != 2) return null
                        val volume = split[0]
                        val relativePath = split[1].trimStart('/')
                        if (volume.equals("primary", ignoreCase = true)) {
                            "/storage/emulated/0/$relativePath"
                        } else {
                            "/storage/$volume/$relativePath"
                        }
                    }
                    "com.android.providers.downloads.documents" -> {
                        val documentId = DocumentsContract.getDocumentId(uri)
                        if (documentId.startsWith("raw:")) {
                            documentId.removePrefix("raw:")
                        } else {
                            queryLegacyDataColumn(context, uri)
                        }
                    }
                    else -> queryLegacyDataColumn(context, uri)
                }
            } else {
                queryLegacyDataColumn(context, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryLegacyDataColumn(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, "_data"),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex("_data")
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
