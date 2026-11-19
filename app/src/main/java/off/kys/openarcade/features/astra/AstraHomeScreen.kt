package off.kys.openarcade.features.astra

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import off.kys.openarcade.features.astra.runtime.RuntimeIntegration
import off.kys.openarcade.features.astra.runtime.WindowsRuntime
import off.kys.openarcade.features.games_launcher.ui.GamesLauncherScreen

private const val ASTRA_PREFS = "astrans_launcher"
private const val KEY_GAME_FOLDER = "game_folder_uri"
private const val KEY_GAME_FILE = "game_file_uri"
private const val KEY_RUNTIME_PACKAGE = "windows_runtime_package"
private const val KEY_DESKTOP_SHORTCUT_PATH = "windows_desktop_shortcut_path"

private val AstraBackground = Color(0xFF080D16)
private val AstraPanel = Color(0xFF131D2B)
private val AstraAccent = Color(0xFF63D9FF)
private val AstraText = Color(0xFFEAF3FF)
private val AstraMuted = Color(0xFF9AABC0)

class AstraHomeScreen : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val preferences = remember(context) {
            context.getSharedPreferences(ASTRA_PREFS, Context.MODE_PRIVATE)
        }
        var folderUri by remember { mutableStateOf(preferences.getString(KEY_GAME_FOLDER, null)) }
        var gameFileUri by remember { mutableStateOf(preferences.getString(KEY_GAME_FILE, null)) }
        var status by remember { mutableStateOf("Pilih folder game atau file executable untuk menyimpan lokasinya.") }

        val initiallyDetectedRuntimes = remember(context) {
            RuntimeIntegration.findInstalledWinlatorBuilds(context)
        }
        var runtimes by remember { mutableStateOf(initiallyDetectedRuntimes) }
        var selectedRuntimePackage by remember {
            mutableStateOf(
                preferences.getString(
                    KEY_RUNTIME_PACKAGE,
                    initiallyDetectedRuntimes.firstOrNull()?.packageName
                ).orEmpty()
            )
        }
        var desktopShortcutPath by remember {
            mutableStateOf(preferences.getString(KEY_DESKTOP_SHORTCUT_PATH, "").orEmpty())
        }

        LaunchedEffect(runtimes) {
            if (runtimes.isNotEmpty() && runtimes.none { it.packageName == selectedRuntimePackage }) {
                val firstRuntime = runtimes.first()
                selectedRuntimePackage = firstRuntime.packageName
                preferences.edit().putString(KEY_RUNTIME_PACKAGE, firstRuntime.packageName).apply()
            }
        }

        val folderPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            if (uri != null) {
                persistReadPermission(context, uri)
                folderUri = uri.toString()
                preferences.edit().putString(KEY_GAME_FOLDER, uri.toString()).apply()
                status = "Folder game berhasil disimpan."
            }
        }

        val gameFilePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                val displayName = queryDisplayName(context, uri)
                val extension = displayName.substringAfterLast('.', "").lowercase()
                if (extension == "exe" || extension == "zip") {
                    persistReadPermission(context, uri)
                    gameFileUri = uri.toString()
                    preferences.edit().putString(KEY_GAME_FILE, uri.toString()).apply()
                    status = "File $displayName berhasil disimpan."
                } else {
                    status = "Pilih file .exe atau .zip. File tidak diubah atau diekstrak oleh launcher."
                }
            }
        }

        val desktopShortcutPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                val displayName = queryDisplayName(context, uri)
                if (!displayName.endsWith(".desktop", ignoreCase = true)) {
                    status = "Pilih file shortcut dengan ekstensi .desktop."
                } else {
                    persistReadPermission(context, uri)
                    val absolutePath = RuntimeIntegration.resolveFilesystemPath(context, uri)
                    if (absolutePath != null) {
                        desktopShortcutPath = absolutePath
                        preferences.edit().putString(KEY_DESKTOP_SHORTCUT_PATH, absolutePath).apply()
                        status = "Shortcut $displayName terpilih."
                    } else {
                        status = "File dipilih, tetapi penyedia file Android tidak memberikan path filesystem. Masukkan path absolut file .desktop secara manual."
                    }
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = AstraBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ASTRA", color = AstraAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Text("NS LAUNCHER", color = AstraText, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    Text("ANDROID", color = AstraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AstraPanel)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("GAME PROFILE / 01", color = AstraAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("NARUTO SHIPPUDEN", color = AstraText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("ULTIMATE NINJA STORM REVOLUTION", color = AstraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Target perangkat: Helio G100 • RAM 8 GB", color = AstraMuted, fontSize = 12.sp)
                        Text("Profil awal: 960 × 540 • target 30 FPS", color = AstraAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AstraPanel)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PENGELOLAAN FILE GAME", color = AstraText, fontWeight = FontWeight.Bold)
                        Text(
                            text = folderUri?.let { "Folder tersimpan: " + (Uri.parse(it).lastPathSegment ?: "folder dipilih") } ?: "Folder game belum dipilih.",
                            color = AstraMuted,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { folderPicker.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AstraAccent, contentColor = AstraBackground)
                        ) {
                            Text("Pilih folder game", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = gameFileUri?.let { "File tersimpan: " + queryDisplayName(context, Uri.parse(it)) } ?: "Belum ada file .exe atau .zip yang dipilih.",
                            color = AstraMuted,
                            fontSize = 12.sp
                        )
                        OutlinedButton(onClick = { gameFilePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Pilih file .EXE / .ZIP", color = AstraAccent)
                        }
                    }
                }

                RuntimeCard(
                    runtimes = runtimes,
                    selectedRuntimePackage = selectedRuntimePackage,
                    desktopShortcutPath = desktopShortcutPath,
                    onSelectRuntime = { runtime: WindowsRuntime ->
                        selectedRuntimePackage = runtime.packageName
                        preferences.edit().putString(KEY_RUNTIME_PACKAGE, runtime.packageName).apply()
                        status = "Runtime " + runtime.label + " dipilih."
                    },
                    onRefreshRuntimes = {
                        runtimes = RuntimeIntegration.findInstalledWinlatorBuilds(context)
                        status = if (runtimes.isEmpty()) {
                            "Tidak ada aplikasi dengan nama Winlator yang terdeteksi."
                        } else {
                            "Pemindaian runtime selesai: " + runtimes.size + " build ditemukan."
                        }
                    },
                    onShortcutPathChange = { path ->
                        desktopShortcutPath = path
                        preferences.edit().putString(KEY_DESKTOP_SHORTCUT_PATH, path).apply()
                    },
                    onPickShortcut = { desktopShortcutPicker.launch(arrayOf("*/*")) },
                    onOpenRuntime = {
                        status = RuntimeIntegration.openRuntime(context, selectedRuntimePackage).message
                    },
                    onLaunchShortcut = {
                        status = RuntimeIntegration.launchDesktopShortcut(
                            context,
                            selectedRuntimePackage,
                            desktopShortcutPath
                        ).message
                    }
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AstraPanel)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("PUSAT KONTROL", color = AstraText, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { navigator.push(AstraProfileScreen()) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Profil grafis & kontrol", color = AstraAccent)
                        }
                        OutlinedButton(onClick = { navigator.push(GamesLauncherScreen()) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Buka pustaka aplikasi Android", color = AstraAccent)
                        }
                    }
                }

                Text(status, color = AstraAccent, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Catatan: AstraNS tidak menyertakan Wine/Box64 di dalam APK. Runtime Windows harus dipasang terpisah. Profil grafis di sini adalah panduan; pengaturan DXVK, Box64, resolusi, VSync, dan kontrol overlay tetap dilakukan di aplikasi runtime.",
                    color = AstraMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RuntimeCard(
    runtimes: List<WindowsRuntime>,
    selectedRuntimePackage: String,
    desktopShortcutPath: String,
    onSelectRuntime: (WindowsRuntime) -> Unit,
    onRefreshRuntimes: () -> Unit,
    onShortcutPathChange: (String) -> Unit,
    onPickShortcut: () -> Unit,
    onOpenRuntime: () -> Unit,
    onLaunchShortcut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AstraPanel)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("RUNTIME WINDOWS / WINE", color = AstraText, fontWeight = FontWeight.Bold)
            Text(
                text = if (runtimes.isEmpty()) {
                    "Winlator belum terdeteksi. Instal Winlator atau varian yang sesuai, lalu tekan deteksi ulang."
                } else {
                    runtimes.size.toString() + " aplikasi Winlator terdeteksi. Pilih build yang ingin dipakai."
                },
                color = AstraMuted,
                fontSize = 12.sp
            )

            runtimes.forEach { runtime ->
                val selected = runtime.packageName == selectedRuntimePackage
                OutlinedButton(
                    onClick = { onSelectRuntime(runtime) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (if (selected) "✓  " else "") + runtime.label,
                        color = if (selected) AstraAccent else AstraText
                    )
                }
            }

            OutlinedButton(onClick = onRefreshRuntimes, modifier = Modifier.fillMaxWidth()) {
                Text("Deteksi ulang runtime", color = AstraAccent)
            }

            Text("SHORTCUT GAME WINLATOR", color = AstraText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(
                "Pilih shortcut .desktop yang sudah diekspor dari Winlator. Jika path tidak terbaca otomatis, masukkan lokasi lengkap file tersebut.",
                color = AstraMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            OutlinedTextField(
                value = desktopShortcutPath,
                onValueChange = onShortcutPathChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Path file .desktop") },
                placeholder = {
                    Text("/storage/emulated/0/Download/Winlator/FrontendShortcuts/Naruto.desktop")
                },
                singleLine = true
            )
            OutlinedButton(onClick = onPickShortcut, modifier = Modifier.fillMaxWidth()) {
                Text("Pilih shortcut .desktop", color = AstraAccent)
            }

            Button(
                onClick = onOpenRuntime,
                enabled = selectedRuntimePackage.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AstraAccent,
                    contentColor = AstraBackground
                )
            ) {
                Text("Buka runtime", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onLaunchShortcut,
                enabled = selectedRuntimePackage.isNotBlank() && desktopShortcutPath.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kirim shortcut ke runtime", color = AstraAccent)
            }
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Some document providers do not offer persistable URI permissions.
    } catch (_: IllegalArgumentException) {
        // Keep the selected URI even when a provider rejects persistence.
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: (uri.lastPathSegment ?: "file dipilih")
    } catch (_: Exception) {
        uri.lastPathSegment ?: "file dipilih"
    }
}
