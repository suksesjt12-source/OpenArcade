package off.kys.openarcade.features.astra

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import off.kys.openarcade.features.games_launcher.ui.GamesLauncherScreen

private const val ASTRA_PREFS = "astrans_launcher"
private const val KEY_GAME_FOLDER = "game_folder_uri"
private const val KEY_GAME_FILE = "game_file_uri"

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
                        Button(
                            onClick = {
                                val launchIntent = findWinlatorLaunchIntent(context)
                                if (launchIntent != null) {
                                    status = "Membuka Winlator. Pilih executable game di dalam Winlator."
                                    context.startActivity(launchIntent)
                                } else {
                                    status = "Winlator belum terdeteksi. Instal runtime Winlator terlebih dahulu."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AstraAccent, contentColor = AstraBackground)
                        ) {
                            Text("Buka Winlator", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(status, color = AstraAccent, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Catatan: AstraNS menyimpan referensi file dan profil. Menjalankan game PC tetap membutuhkan Winlator atau runtime kompatibilitas lain. Profil grafis di aplikasi ini adalah panduan; pengaturan runtime dilakukan di Winlator.",
                    color = AstraMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
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

@Suppress("DEPRECATION")
private fun findWinlatorLaunchIntent(context: Context): Intent? {
    val packageManager = context.packageManager
    val applications: List<ApplicationInfo> = try {
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }
    } catch (_: Exception) {
        emptyList()
    }

    return applications
        .filter { application ->
            packageManager.getApplicationLabel(application).toString().contains("winlator", ignoreCase = true)
        }
        .sortedBy { application ->
            packageManager.getApplicationLabel(application).toString().length
        }
        .firstNotNullOfOrNull { application ->
            packageManager.getLaunchIntentForPackage(application.packageName)
        }
}
