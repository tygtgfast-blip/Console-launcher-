package com.example.consolelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsoleLauncherApp()
        }
    }
}

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConsoleLauncherApp() {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }

    // Сохраненные пакеты в карусели
    var savedPackages by remember {
        val raw = prefs.getString("saved_games", "") ?: ""
        mutableStateOf(if (raw.isEmpty()) listOf() else raw.split(","))
    }

    // Настройки интерфейса
    var dockPosition by remember { mutableStateOf(prefs.getString("dock_pos", "CENTER") ?: "CENTER") }
    var cardSize by remember { mutableStateOf(prefs.getString("card_size", "MEDIUM") ?: "MEDIUM") }

    // Состояния диалогов
    var showAppPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedGameForEdit by remember { mutableStateOf<String?>(null) }
    var customCoverTrigger by remember { mutableStateOf(0) }

    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    fun saveGames(list: List<String>) {
        savedPackages = list
        prefs.edit().putString("saved_games", list.joinToString(",")).apply()
    }

    // Средство выбора обложки из галереи
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val pkg = selectedGameForEdit
        if (uri != null && pkg != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "cover_$pkg.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                customCoverTrigger++
                Toast.makeText(context, "Обложка обновлена!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения картинки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val (cardWidth, cardHeight) = when (cardSize) {
        "SMALL" -> 130.dp to 180.dp
        "LARGE" -> 220.dp to 300.dp
        else -> 170.dp to 240.dp
    }

    val verticalAlignment = when (dockPosition) {
        "TOP" -> Alignment.Top
        "BOTTOM" -> Alignment.Bottom
        else -> Alignment.CenterVertically
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
    ) {
        // Верхняя панель (Консольный статус-бар)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONSOLE LAUNCHER",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 2.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                IconButton(onClick = { showAppPicker = true }) {
                    Text("➕", fontSize = 20.sp)
                }
                IconButton(onClick = { showSettings = true }) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }
        }

        // Центральная карусель игр
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (dockPosition == "TOP") 70.dp else 0.dp, bottom = if (dockPosition == "BOTTOM") 40.dp else 0.dp),
            contentAlignment = verticalAlignment
        ) {
            if (savedPackages.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Карусель пуста",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите ➕ вверху, чтобы добавить установленные игры",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(savedPackages) { pkg ->
                        key(pkg, customCoverTrigger) {
                            val customCoverFile = File(context.filesDir, "cover_$pkg.jpg")
                            val appInfo = try {
                                pm.getApplicationInfo(pkg, 0)
                            } catch (e: Exception) {
                                null
                            }

                            val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
                            val appIcon = appInfo?.let { pm.getApplicationIcon(it) }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .combinedClickable(
                                        onClick = {
                                            val intent = pm.getLaunchIntentForPackage(pkg)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "Не удалось запустить игру", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onLongClick = {
                                            selectedGameForEdit = pkg
                                        }
                                    )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (customCoverFile.exists()) {
                                        AsyncImage(
                                            model = customCoverFile,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            if (appIcon != null) {
                                                AsyncImage(
                                                    model = appIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = appName,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Меню действий по долгому нажатию на карточку
        selectedGameForEdit?.let { pkg ->
            AlertDialog(
                onDismissRequest = { selectedGameForEdit = null },
                title = { Text("Настройка игры") },
                text = { Text("Выберите действие для этой игры:") },
                confirmButton = {
                    TextButton(onClick = {
                        imagePicker.launch("image/*")
                        selectedGameForEdit = null
                    }) {
                        Text("🖼️ Сменить обложку")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        saveGames(savedPackages.filter { it != pkg })
                        File(context.filesDir, "cover_$pkg.jpg").delete()
                        selectedGameForEdit = null
                    }) {
                        Text("🗑️ Убрать из карусели", color = Color.Red)
                    }
                }
            )
        }

        // Диалог выбора приложений (➕)
        if (showAppPicker) {
            val allApps = remember {
                val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                pm.queryIntentActivities(intent, 0).map {
                    InstalledApp(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = it.loadIcon(pm)
                    )
                }.sortedBy { it.name.lowercase() }
            }

            AlertDialog(
                onDismissRequest = { showAppPicker = false },
                title = { Text("Добавить в консоль") },
                text = {
                    LazyColumn(modifier = Modifier.height(350.dp)) {
                        items(allApps) { app ->
                            val isAdded = savedPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = app.name,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Checkbox(
                                    checked = isAdded,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            saveGames(savedPackages + app.packageName)
                                        } else {
                                            saveGames(savedPackages.filter { it != app.packageName })
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showAppPicker = false }) {
                        Text("Готово")
                    }
                }
            )
        }

        // Диалог настроек (⚙️)
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Настройки кастомизации") },
                text = {
                    Column {
                        Text("Положение ленты на экране:", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("TOP" to "Сверху", "CENTER" to "По центру", "BOTTOM" to "Снизу").forEach { (pos, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = dockPosition == pos,
                                        onClick = {
                                            dockPosition = pos
                                            prefs.edit().putString("dock_pos", pos).apply()
                                        }
                                    )
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Размер карточек:", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("SMALL" to "Мал.", "MEDIUM" to "Сред.", "LARGE" to "Бол.").forEach { (size, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = cardSize == size,
                                        onClick = {
                                            cardSize = size
                                            prefs.edit().putString("card_size", size).apply()
                                        }
                                    )
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showSettings = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}
