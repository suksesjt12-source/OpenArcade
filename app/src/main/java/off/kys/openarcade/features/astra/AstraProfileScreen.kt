package off.kys.openarcade.features.astra

import android.content.Context
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

private const val PROFILE_PREFS = "astrans_launcher"
private const val KEY_RESOLUTION = "profile_resolution"
private const val KEY_FPS = "profile_fps"
private const val KEY_TOUCH_GUIDE = "touch_control_guide"

private val ProfileBackground = Color(0xFF080D16)
private val ProfilePanel = Color(0xFF131D2B)
private val ProfileAccent = Color(0xFF63D9FF)
private val ProfileText = Color(0xFFEAF3FF)
private val ProfileMuted = Color(0xFF9AABC0)

class AstraProfileScreen : Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val preferences = remember(context) {
            context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
        }
        var resolution by remember { mutableStateOf(preferences.getString(KEY_RESOLUTION, "960 × 540") ?: "960 × 540") }
        var fps by remember { mutableStateOf(preferences.getInt(KEY_FPS, 30)) }
        var showControlGuide by remember { mutableStateOf(preferences.getBoolean(KEY_TOUCH_GUIDE, true)) }

        Surface(modifier = Modifier.fillMaxSize(), color = ProfileBackground) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("ASTRA / PROFIL", color = ProfileAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Pengaturan permainan", color = ProfileText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ProfilePanel)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("RESOLUSI TARGET", color = ProfileText, fontWeight = FontWeight.Bold)
                        Text("Pilih resolusi yang akan dijadikan panduan saat mengatur container di Winlator.", color = ProfileMuted, fontSize = 12.sp)
                        listOf("854 × 480", "960 × 540", "1280 × 720").forEach { option ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = resolution == option,
                                    onClick = {
                                        resolution = option
                                        preferences.edit().putString(KEY_RESOLUTION, option).apply()
                                    }
                                )
                                Text(option, color = ProfileText)
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ProfilePanel)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TARGET FRAME RATE", color = ProfileText, fontWeight = FontWeight.Bold)
                        listOf(30, 60).forEach { option ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = fps == option,
                                    onClick = {
                                        fps = option
                                        preferences.edit().putInt(KEY_FPS, option).apply()
                                    }
                                )
                                Text("$option FPS", color = ProfileText)
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = ProfilePanel)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("PANDUAN KONTROL SENTUH", color = ProfileText, fontWeight = FontWeight.Bold)
                        Text(
                            "Simpan preferensi untuk panduan tata letak kontrol pada launcher. Ini bukan overlay di atas Winlator.",
                            color = ProfileMuted,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Panduan kontrol aktif", color = ProfileText)
                            Switch(
                                checked = showControlGuide,
                                onCheckedChange = {
                                    showControlGuide = it
                                    preferences.edit().putBoolean(KEY_TOUCH_GUIDE, it).apply()
                                }
                            )
                        }
                    }
                }

                Text(
                    "Profil tersimpan di perangkat ini. AstraNS belum mengubah resolusi, VSync, DXVK, Box64, atau tombol overlay Winlator secara otomatis. Terapkan nilainya secara manual di runtime.",
                    color = ProfileMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Kembali ke beranda")
                }
            }
        }
    }
}
