package org.dergigi.fishyfishy

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val OceanInk = Color(0xFF193E3D)
private val Gold = Color(0xFFF0C86C)
private val Shell = RoundedCornerShape(24.dp)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val language = newBase.getSharedPreferences("preferences", 0).getString("language", "en") ?: "en"
        val locale = AppStrings(language).locale
        val configuration = android.content.res.Configuration(newBase.resources.configuration).apply { setLocale(locale) }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val language = getSharedPreferences("preferences", 0).getString("language", "en") ?: "en"
            val strings = remember(language) {
                if (language == "en") AppStrings(language)
                else AppStrings.parse(language, assets.open("i18n/$language.json").bufferedReader().use { it.readText() })
            }
            val translatedGuide = remember(strings) { guide.map(strings::species) }
            CompositionLocalProvider(LocalStrings provides strings, LocalGuide provides translatedGuide) {
                FishyTheme { FishyApp() }
            }
        }
    }
}

private fun Species.display(language: String) = when (language) { "pt" -> portuguese; "de" -> german; else -> name }

@Composable private fun LanguageMenu(language: String, select: (String) -> Unit) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(Triple("en", "🇬🇧", "English"), Triple("pt", "🇵🇹", "Português"), Triple("de", "🇩🇪", "Deutsch"))
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = strings("App language") + ": " + options.first { it.first == language }.third }) {
            Text(options.first { it.first == language }.second, fontSize = 24.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, flag, name) ->
                DropdownMenuItem(text = { Text("$flag  $name") },
                    trailingIcon = { if (language == code) Icon(Icons.Rounded.Check, null) },
                    onClick = { expanded = false; select(code) })
            }
        }
    }
}

@Composable private fun FishyApp(model: JournalModel = viewModel()) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("preferences", 0) }
    val language = strings.language
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var about by rememberSaveable { mutableStateOf(false) }
    var quiz by rememberSaveable { mutableStateOf(false) }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var seed by rememberSaveable { mutableStateOf<String?>(null) }
    val speak = rememberSpeaker()
    val lifecycle = (context as ComponentActivity).lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(foreground) { if (!foreground) speak.stop() }
    LaunchedEffect(detail, quiz, about, editor) { speak.stop() }
    LaunchedEffect(foreground, model.folderUri) {
        if (foreground) {
            model.refresh()
            while (true) { delay(5000); if (model.folderUri != null) model.refresh() }
        }
    }
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) model.selectFolder(uri)
    }
    val snackbar = remember { SnackbarHostState() }
    val confirmed = model.trips.flatMap { it.sightings - it.uncertain }.toSet()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) model.exportBackup { raw ->
            requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter().use { it.write(raw) }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.importBackup {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 5_000_000)
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
        }
    }
    LaunchedEffect(model.message) { model.message?.let { snackbar.showSnackbar(strings.error(it)); model.message = null } }
    fun addSwim(id: String? = null) { model.clearOperationError(); seed = id; editor = "new-${UUID.randomUUID()}" }
    BackHandler(detail != null || about || quiz) { when { quiz -> quiz = false; about -> about = false; else -> detail = null } }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= 700.dp
        Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (!wide && detail == null && !about && !quiz) NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                listOf(strings("Explore") to Icons.Rounded.Explore, strings("Our swims") to Icons.Rounded.Waves, strings("Collection") to Icons.Rounded.AutoAwesome).forEachIndexed { i, item ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(item.second, null) }, label = { Text(item.first) })
                }
            } },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide) NavigationRail(containerColor = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxHeight().padding(top = 18.dp), header = {
                    Image(painterResource(R.drawable.app_icon), "FishyFishy", Modifier.size(48.dp).clip(CircleShape))
                    Spacer(Modifier.height(26.dp))
                }) {
                    listOf(strings("Explore") to Icons.Rounded.Explore, strings("Our swims") to Icons.Rounded.Waves, strings("Collection") to Icons.Rounded.AutoAwesome).forEachIndexed { i, item ->
                        NavigationRailItem(selected = tab == i, onClick = { tab = i; detail = null; about = false; quiz = false }, icon = { Icon(item.second, null) }, label = { Text(item.first) }, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.widthIn(max = 1100.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (detail != null || about || quiz) IconButton(onClick = { detail = null; about = false; quiz = false }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings("Back")) }
                        else Image(painterResource(R.drawable.app_icon), null, Modifier.size(36.dp).clip(CircleShape))
                        Text("fishyfishy", fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, modifier = Modifier.padding(start = 10.dp).weight(1f), letterSpacing = (-1).sp)
                        LanguageMenu(language) { selected ->
                            if (selected != language) {
                                speak.stop()
                                prefs.edit().putString("language", selected).commit()
                                (context as ComponentActivity).recreate()
                            }
                        }
                        IconButton(onClick = { about = true }) { Icon(Icons.Rounded.Info, strings("Storage, sources and backups"), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (!model.loading && (model.loadError != null || model.conflicts.isNotEmpty())) {
                        TextButton(onClick = { about = true }) {
                            Text(if (model.loadError != null) strings("Journal storage needs attention · Open storage") else strings("%d swim conflicts to review · Open storage", model.conflicts.size), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (model.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else Box(Modifier.widthIn(max = 1100.dp).fillMaxSize()) {
                        when {
                            about -> AboutScreen(model, { chooseFolder.launch(model.folderUri?.toUri()) }, { export.launch("fishyfishy-${LocalDate.now()}.json") }, { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                            quiz -> QuizScreen(language, speak)
                            detail != null -> SpeciesScreen(guide.first { it.id == detail }, language, confirmed.contains(detail), speak, { addSwim(detail) })
                            tab == 0 -> ExploreScreen(language, confirmed, model.trips.size, { detail = it }, { addSwim() })
                            tab == 1 -> JournalScreen(model, { addSwim() }, { model.clearOperationError(); editor = it.id; seed = null })
                            else -> CollectionScreen(confirmed, language, { detail = it }, { quiz = true })
                        }
                    }
                }
            }
        }
    }
    if (editor != null) {
        key(editor) {
            // Freeze both the draft's original data and its revision IDs, including across process recreation.
            val original by rememberSaveable { mutableStateOf(model.trips.find { it.id == editor }?.let { JournalCodec.encode(listOf(it)) }) }
            val expected by rememberSaveable { mutableStateOf(model.headIds(editor!!).toList()) }
            val existing = remember(original) { original?.let { JournalCodec.decode(it).single() } }
            SwimEditor(existing, seed, language, model.busy, model.loadError, model.operationError, { editor = null },
                { model.save(it, expected.toSet()) { editor = null; detail = null; tab = 1 } },
                { trip -> model.delete(trip, expected.toSet()) { editor = null } })
        }
    }
}

@Composable private fun Eyebrow(text: String, light: Boolean = false) {
    val strings = LocalStrings.current
    Text(text.uppercase(strings.locale), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = if (light) Color(0xFFB9E0D0) else MaterialTheme.colorScheme.primary)
}
@Composable private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 37.sp, color = MaterialTheme.colorScheme.onSurface)
}
@Composable private fun FishArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color(0xFF2C8B80).copy(alpha = .5f), w * .5f, Offset(w * .88f, h * .6f))
        for (i in 0..5) drawCircle(Color.White.copy(alpha = .15f), (3 + i % 3 * 3).dp.toPx(), Offset(w * (.15f + i * .14f), h * (.12f + i % 3 * .12f)))
        fun fish(x: Float, y: Float, s: Float, c: Color) {
            val tail = Path().apply { moveTo(x+s*.65f,y); lineTo(x+s*1.05f,y-s*.35f); lineTo(x+s*.98f,y+s*.35f); close() }
            drawPath(tail,c)
            drawOval(c, Offset(x-s*.55f,y-s*.31f),Size(s*1.35f,s*.62f))
            drawCircle(OceanInk, s*.045f,Offset(x-s*.32f,y-s*.04f))
            drawLine(c.copy(alpha=.5f),Offset(x,y),Offset(x+s*.15f,y+s*.22f),s*.1f)
        }
        fish(w*.57f,h*.49f,w*.38f,Gold)
        fish(w*.27f,h*.78f,w*.20f,Color(0xFFA4D5C3))
        fish(w*.81f,h*.85f,w*.13f,Color(0xFFF09E80))
    }
}

@Composable private fun ExploreScreen(language: String, confirmed: Set<String>, swims: Int, open: (String) -> Unit, add: () -> Unit) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered = guide.filter { strings.matches(it, query) && (filter == "All" || it.group == filter || filter in it.tags) }
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Eyebrow(strings("Madeira · your little ocean club"))
                Spacer(Modifier.height(10.dp))
                Heading(strings("Big wonders.\nLittle explorers."))
                Text(strings("Learn their names. Remember your adventures."), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 9.dp, bottom = 20.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().clip(Shell).background(OceanInk)) {
                    val roomy = maxWidth > 500.dp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(24.dp)) {
                            Eyebrow(strings("Just add curiosity"), true)
                            Text(if (swims == 0) strings("Your first swim\nstarts a story.") else strings("More sea.\nMore memories."), fontFamily = FontFamily.Serif, fontSize = 27.sp, lineHeight = 32.sp, color = Color.White, modifier = Modifier.padding(vertical = 14.dp))
                            Button(onClick = add, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = OceanInk)) {
                                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(strings("Log a swim"), fontWeight = FontWeight.Bold)
                            }
                        }
                        FishArt(Modifier.width(if (roomy) 260.dp else 112.dp).height(208.dp))
                    }
                }
                Spacer(Modifier.height(26.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Eyebrow(strings("Meet the neighbours")); Text(strings("Madeira field guide"), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp)) }
                    Text(strings("%d creatures", guide.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text(strings("Name, colour, or a clue…")) }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, strings("Clear search")) } }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Fish", "Critters", "Stripes", "Blue", "Schools", "Sand", "Shell", "Legs").forEach { tag -> FilterChip(selected = filter == tag, onClick = { filter = tag }, label = { Text(strings(tag)) }) }
                }
            }
        }
        items(filtered, key = { it.id }) { species -> SpeciesCard(species, language, species.id in confirmed, { open(species.id) }) }
        if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyState(strings("A little mystery…"), strings("Try another name or clue. This is a small starter guide, so your creature might not be here yet.")) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(strings("Look with your eyes. Leave only bubbles.\nPhotos are clues: colours can vary with age and light."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(vertical = 14.dp))
        }
    }
}

@Composable private fun SpeciesCard(species: Species, language: String, spotted: Boolean, open: () -> Unit) {
    val strings = LocalStrings.current
    Card(onClick = open, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box {
            Image(painterResource(species.image), species.display(language), Modifier.fillMaxWidth().aspectRatio(1.25f), contentScale = ContentScale.Crop)
            if (spotted) Surface(Modifier.align(Alignment.TopEnd).padding(8.dp), shape = CircleShape, color = Gold) { Icon(Icons.Rounded.Check, strings("Spotted"), Modifier.padding(6.dp).size(16.dp), tint = OceanInk) }
        }
        Column(Modifier.padding(14.dp)) {
            Text(species.display(language), fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
            Text(species.scientific, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp), lineHeight = 15.sp)
        }
    }
}

@Composable private fun SpeciesScreen(species: Species, language: String, spotted: Boolean, speak: Speaker, add: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val photos = listOf(GuidePhoto(species.image, species.id, species.photoLabel)) + species.otherPhotos
    var photoIndex by rememberSaveable(species.id) { mutableStateOf(0) }
    val photo = photos[photoIndex.coerceIn(photos.indices)]
    var showPhoto by rememberSaveable(species.id) { mutableStateOf(false) }
    val credits = remember(photo.creditId) {
        val all = JSONArray(context.assets.open("photo-credits.json").bufferedReader().use { it.readText() })
        (0 until all.length()).map { all.getJSONObject(it) }.first { it.getString("id") == photo.creditId }
    }
    if (showPhoto) PhotoViewer(photo, species.display(language),
        "${credits.getString("author")} · ${credits.getString("license")}") { showPhoto = false }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Image(painterResource(photo.image), "${species.display(language)}: ${photo.label}", Modifier.fillMaxWidth().heightIn(max = 430.dp).aspectRatio(1.4f).clip(Shell).clickable(onClickLabel = strings("Open photo to zoom")) { showPhoto = true }, contentScale = ContentScale.Crop)
        }
        item { Text(strings("Tap the photo to zoom in"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
        item {
            Eyebrow(if (spotted) strings("A familiar face · spotted by you") else if (species.comparisonNote != null) strings("A lookalike to compare") else strings("Meet a Madeira neighbour"))
            Spacer(Modifier.height(9.dp)); Heading(species.display(language))
            Text(species.scientific, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        }
        item {
            Surface(shape = Shell, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(18.dp)) {
                    Eyebrow(strings("One creature. Many names."))
                    listOf(Triple("English", species.name, "en"), Triple("Português", species.portuguese, "pt"), Triple("Deutsch", species.german, "de")).forEach { (label, name, code) ->
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) }
                            SpeakButton(speak, name, code, strings("Listen in %s", label))
                        }
                    }
                    Text(strings("Scientific · %s", species.scientific), color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 14.dp))
                }
            }
        }
        if (photos.size > 1) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEachIndexed { index, option -> FilterChip(selected = photoIndex == index, onClick = { photoIndex = index }, label = { Text(option.label) }) }
            }
        }
        species.comparisonNote?.let { note -> item { FactBlock(strings("Compare carefully"), note, Icons.Rounded.Search) } }
        item { FactBlock(strings("How to spot it"), species.clues, Icons.Rounded.Search) }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = Shell) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Eyebrow(strings("Little ocean lesson")); Spacer(Modifier.weight(1f)); SpeakButton(speak, species.fact, language, strings("Read this fact aloud")) }
                    Text(species.fact, fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 30.sp)
                }
            }
        }
        item { FactBlock(strings("Where to look"), species.habitat, Icons.Rounded.Waves) }
        item { FactBlock(strings("Your explorer mission"), species.mission, Icons.Rounded.AutoAwesome) }
        filmFor(species.id, language)?.let { film ->
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = Shell) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Column { Eyebrow(strings("Watch together")); Text("${film.publisher} · ${film.languageLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                        }
                        Text(film.title, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp)
                        Text(strings(film.topic), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(strings(film.note), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                        FilledTonalButton(onClick = {
                            speak.stop()
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, film.url.toUri())) }
                            catch (_: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, strings("Install YouTube or a browser to watch this film."), Toast.LENGTH_LONG).show()
                            }
                        }) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(strings("Watch on YouTube ↗")) }
                        Text(strings("Opens YouTube or your browser · Internet needed"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
        item { Button(onClick = add, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(strings("Log a swim with this creature")) } }
        item {
            Text(strings("Not sure? Keep it as a possible sighting. Never touch, chase or feed wildlife."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, species.source.toUri())) }) { Text(strings("Read the species reference ↗")) }
            Text(strings("Photo: %s · %s\n%s", credits.getString("author"), credits.getString("license"), strings(credits.getString("changes"))), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, credits.getString("source").toUri())) }) { Text(strings("Photo source & licence ↗"), fontSize = 12.sp) }
        }
    }
}

@Composable private fun FactBlock(title: String, body: String, icon: ImageVector) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 3.dp).size(24.dp))
        Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 23.sp, modifier = Modifier.padding(top = 6.dp)) }
    }
}
@Composable private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().clip(Shell).background(MaterialTheme.colorScheme.surfaceVariant).padding(28.dp)) {
        Icon(Icons.Rounded.Waves, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text(title, fontFamily = FontFamily.Serif, fontSize = 26.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 23.sp)
    }
}

@Composable private fun JournalScreen(model: JournalModel, add: () -> Unit, edit: (Trip) -> Unit) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Eyebrow(strings("Our saltwater stories")); Spacer(Modifier.height(10.dp)); Heading(strings("Every swim,\na little adventure.")) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("${model.trips.size}", strings("swims"), Modifier.weight(1f))
                Stat("${model.trips.sumOf { it.minutes }}", strings("minutes in the sea"), Modifier.weight(1f))
            }
        }
        model.loadError?.let { error -> item { Text(strings.error(error), color = MaterialTheme.colorScheme.error) } }
        item { Button(onClick = add, enabled = model.loadError == null && !model.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text(strings("Log a swim")) } }
        if (model.trips.isEmpty()) item { EmptyState(strings("A journal full of possibility"), strings("Where did you go? What did you see? Save your first swim together, even if the fish are still a mystery.")) }
        items(model.trips, key = { it.id }) { trip ->
            Card(onClick = { edit(trip) }, shape = Shell, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(20.dp)) {
                    Eyebrow(LocalDate.parse(trip.date).format(DateTimeFormatter.ofPattern("d MMM yyyy", strings.locale)))
                    Text(trip.place, fontSize = 24.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    Text(strings("%d min · %s", trip.minutes, strings.creatures(trip.sightings.size)) + if (trip.uncertain.isNotEmpty()) strings(" · %d to check", trip.uncertain.size) else "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    if (trip.sightings.isNotEmpty()) Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        guide.filter { it.id in trip.sightings }.take(5).forEach { s -> Image(painterResource(s.image), s.display(strings.language) + if (s.id in trip.uncertain) strings(", possible sighting") else "", Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop) }
                        if (trip.sightings.size > 5) Text("+${trip.sightings.size - 5}", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.primary)
                    }
                    if (trip.notes.isNotBlank()) Text(trip.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, modifier = Modifier.padding(top = 14.dp), lineHeight = 21.sp)
                }
            }
        }
        item { Text(strings("Choose a journal folder in the ⓘ menu to sync your swims between devices with Syncthing."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
    }
}
@Composable private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(Shell).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)) {
        Text(value, fontSize = 32.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable private fun CollectionScreen(confirmed: Set<String>, language: String, open: (String) -> Unit, quiz: () -> Unit) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Eyebrow(strings("Your ocean discoveries")); Spacer(Modifier.height(10.dp)); Heading(strings("Familiar fins\n& new friends."))
                Text(strings("%d of %d creatures spotted", confirmed.size, guide.size), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
                LinearProgressIndicator(progress = { confirmed.size.toFloat() / guide.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(24.dp))
                Surface(shape = Shell, color = Gold.copy(alpha = .35f)) {
                    Column(Modifier.padding(22.dp)) {
                        Eyebrow(strings("A little game for dry land"))
                        Text(strings("Who’s that fish?"), fontSize = 25.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                        Text(strings("Look at a photo. Learn a name. Play together."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = quiz, modifier = Modifier.padding(top = 14.dp)) { Text(strings("Let’s play")); Spacer(Modifier.width(8.dp)); Icon(Icons.Rounded.PlayArrow, null) }
                    }
                }
                Text(if (confirmed.isEmpty()) strings("Your collection is waiting") else strings("Spotted by you"), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 26.dp, bottom = 8.dp))
                if (confirmed.isEmpty()) Text(strings("Log a swim and mark the creatures you recognised. Possible sightings stay in your journal until you’re sure."), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 23.sp)
            }
        }
        items(guide.filter { it.id in confirmed }, key = { it.id }) { s -> SpeciesCard(s, language, true) { open(s.id) } }
    }
}

@Composable private fun QuizScreen(language: String, speak: Speaker) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    var question by rememberSaveable { mutableStateOf(guide.random().id) }
    var options by rememberSaveable { mutableStateOf((guide.filterNot { it.id == question }.shuffled().take(3).map { it.id } + question).shuffled()) }
    var answered by rememberSaveable { mutableStateOf<String?>(null) }
    val fish = guide.first { it.id == question }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Eyebrow(strings("Look closely. Take your time.")); Heading(strings("Who’s that creature?"))
        Image(painterResource(fish.image), strings("Mystery sea creature. Choose its name below."), Modifier.fillMaxWidth().heightIn(max = 400.dp).aspectRatio(1.4f).clip(Shell), contentScale = ContentScale.Crop)
        options.forEach { id ->
            val option = guide.first { it.id == id }
            OutlinedButton(onClick = { answered = id; speak.speak(option.display(language), language) }, enabled = answered == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), colors = ButtonDefaults.outlinedButtonColors(disabledContainerColor = if (answered != null && id == question) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, disabledContentColor = MaterialTheme.colorScheme.onSurface)) {
                if (answered != null && id == question) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)) }
                Text(option.display(language))
            }
        }
        if (answered != null) {
            Text(if (answered == question) strings("You found it!") else strings("A good chance to learn a new name!"), fontSize = 23.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Text("${fish.name} · ${fish.portuguese}\n${fish.german}\n${fish.scientific}", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
            Text(fish.fact, lineHeight = 23.sp)
            Button(onClick = {
                question = guide.filterNot { it.id == question }.random().id
                options = (guide.filterNot { it.id == question }.shuffled().take(3).map { it.id } + question).shuffled(); answered = null
            }, modifier = Modifier.fillMaxWidth()) { Text(strings("Another creature")) }
        }
    }
}

@Composable private fun AboutScreen(model: JournalModel, chooseFolder: () -> Unit, export: () -> Unit, restore: () -> Unit) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Eyebrow(strings("Made for curious little humans")); Heading(strings("A pocketful\nof ocean wonder."))
        Text(strings("FishyFishy is a snorkeling journal for kids and their grown-ups. Made for discovering the ocean around Madeira, one swim at a time."), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
        FactBlock(strings("Yours, even offline"), strings("No account, ads or tracking. Your swims are saved as files. You choose whether to sync them with another app. The photos and guide are included in the app. Read-aloud uses installed offline Android voices."), Icons.Rounded.FavoriteBorder)
        StorageSection(model, chooseFolder)
        FactBlock(strings("Keep your memories"), strings("Export a backup before changing phones or uninstalling. Restore adds missing swims and keeps existing swims and deletions. If you use Syncthing, sync the whole journal folder instead."), Icons.Rounded.SaveAlt)
        Button(onClick = export, enabled = !model.busy && model.loadError == null, modifier = Modifier.fillMaxWidth()) { Text(strings("Export journal backup")) }
        OutlinedButton(onClick = restore, enabled = !model.busy && model.loadError == null, modifier = Modifier.fillMaxWidth()) { Text(strings("Restore journal backup")) }
        model.loadError?.let { Text(strings.error(it), color = MaterialTheme.colorScheme.error) }
        FactBlock(strings("Be a kind ocean visitor"), strings("Explore with a grown-up, give wildlife space, and leave animals and shells where they belong. There is always something new to notice."), Icons.Rounded.Waves)
        Text(strings("About the guide"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(strings("A coastal guide with %d entries, not every creature in Madeira. Sightings vary with habitat, depth and season. Compare several clues; a photo alone cannot confirm an identification. Names and lessons are available in English, Portuguese and German, with scientific names alongside them. Common names vary by region; some references use older scientific synonyms.", guide.size), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 23.sp)
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://ifcn.madeira.gov.pt/en/areas-protegidas/rocha-do-navio/valores-naturais.html".toUri())) }) { Text(strings("Madeira wildlife · IFCN ↗")) }
        Text(strings("Species references and photo credits appear on each creature’s page. Photos are reproduced under their individual Creative Commons licences; app code is MIT licensed."), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 23.sp)
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/dergigi/fishyfishy".toUri())) }) { Text(strings("Source code & feedback ↗")) }
        Text(strings("FishyFishy %s · Made with love for the sea", BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
    }
}

@Composable private fun SwimEditor(existing: Trip?, seed: String?, language: String, busy: Boolean, loadError: String?, operationError: String?, close: () -> Unit, save: (Trip) -> Unit, delete: (Trip) -> Unit) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    val context = LocalContext.current
    val draftId by rememberSaveable { mutableStateOf(existing?.id ?: UUID.randomUUID().toString()) }
    var date by rememberSaveable { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var place by rememberSaveable { mutableStateOf(existing?.place ?: "") }
    var duration by rememberSaveable { mutableStateOf(existing?.minutes?.toString() ?: "30") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var selected by rememberSaveable { mutableStateOf(existing?.sightings?.toList() ?: listOfNotNull(seed)) }
    var unsure by rememberSaveable { mutableStateOf(existing?.uncertain?.toList() ?: emptyList()) }
    var search by rememberSaveable { mutableStateOf("") }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var discarding by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val initialSelected = existing?.sightings ?: setOfNotNull(seed)
    val dirty = date != (existing?.date ?: LocalDate.now().toString()) || place != (existing?.place ?: "") || duration != (existing?.minutes?.toString() ?: "30") || notes != (existing?.notes ?: "") || selected.toSet() != initialSelected || unsure.toSet() != (existing?.uncertain ?: emptySet<String>())
    fun dismiss() { if (!busy) { if (dirty) discarding = true else close() } }
    Dialog(onDismissRequest = { dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { dismiss() }, enabled = !busy) { Icon(Icons.Rounded.Close, strings("Close swim")) }
                    Text(if (existing == null) strings("A new adventure") else strings("Your adventure"), fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (existing != null) IconButton(onClick = { deleting = true }, enabled = !busy) { Icon(Icons.Rounded.DeleteOutline, strings("Delete swim")) }
                }
                LazyColumn(Modifier.weight(1f).widthIn(max = 800.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Eyebrow(strings("The sea was full of stories")); Spacer(Modifier.height(8.dp)); Heading(strings("Tell us about\nyour swim.")) }
                    item {
                        OutlinedTextField(place, { if (it.length <= 150) place = it }, label = { Text(strings("Where did you go?")) }, placeholder = { Text(strings("e.g. Garajau, Madeira")) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = !busy)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                val d = LocalDate.parse(date)
                                DatePickerDialog(context, { _, y, m, day -> date = LocalDate.of(y, m + 1, day).toString() }, d.year, d.monthValue - 1, d.dayOfMonth).apply {
                                    datePicker.maxDate = System.currentTimeMillis(); show()
                                }
                            }, modifier = Modifier.weight(1f).heightIn(min = 60.dp), enabled = !busy) { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", strings.locale)), fontSize = 13.sp) }
                            OutlinedTextField(duration, { if (it.length <= 3 && it.all(Char::isDigit)) duration = it }, label = { Text(strings("Minutes")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.width(115.dp), shape = RoundedCornerShape(16.dp), enabled = !busy)
                        }
                    }
                    item { Text(strings("Who did you meet?"), fontSize = 24.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold); Text(strings("Tap a creature to add it. It’s okay to be unsure."), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp)) }
                    item { OutlinedTextField(search, { search = it }, placeholder = { Text(strings("Find a creature…")) }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) }
                    items(guide.filter { strings.matches(it, search) }, key = { it.id }) { s ->
                        val checked = s.id in selected
                        Surface(shape = RoundedCornerShape(18.dp), color = if (checked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column {
                                Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { if (checked) { selected = selected - s.id; unsure = unsure - s.id } else selected = selected + s.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Image(painterResource(s.image), null, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                    Column(Modifier.weight(1f)) { Text(s.display(language), fontWeight = FontWeight.Bold); Text(s.scientific, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    Checkbox(checked = checked, onCheckedChange = null)
                                }
                                if (checked) Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings("Not sure yet"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Switch(checked = s.id in unsure, onCheckedChange = { unsure = if (it) unsure + s.id else unsure - s.id }, enabled = !busy)
                                }
                            }
                        }
                    }
                    if (guide.none { strings.matches(it, search) }) item { Text(strings("Not in our starter guide? Describe it in your notes below."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item { OutlinedTextField(notes, { if (it.length <= 10000) notes = it }, label = { Text(strings("A memory to keep")) }, placeholder = { Text(strings("A mystery fish? A funny moment? Tell your story…")) }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), enabled = !busy) }
                    item { Text(strings("Selected: %d · To check: %d", selected.size, unsure.size), color = MaterialTheme.colorScheme.primary) }
                    (error ?: loadError ?: operationError)?.let { message -> item { Text(strings.error(message), color = MaterialTheme.colorScheme.error) } }
                }
                Button(onClick = {
                    val trip = Trip(draftId, date, place.trim(), duration.toIntOrNull() ?: 0, notes.trim(), selected.toSet(), unsure.toSet())
                    try { trip.validate(); error = null; save(trip) } catch (e: Exception) { error = e.message ?: strings("Please check your swim details.") }
                }, enabled = !busy && loadError == null, modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 54.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = LocalContentColor.current, strokeWidth = 2.dp) else { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text(strings("Save our swim")) }
                }
            }
        }
        if (deleting && existing != null) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(strings("Delete this swim?")) }, text = { Text(strings("This removes the swim and its sightings from your journal.")) }, confirmButton = { TextButton(onClick = { deleting = false; delete(existing) }) { Text(strings("Delete swim")) } }, dismissButton = { TextButton(onClick = { deleting = false }) { Text(strings("Keep it")) } })
        if (discarding) AlertDialog(onDismissRequest = { discarding = false }, title = { Text(strings("Leave without saving?")) }, text = { Text(strings("Your changes to this swim will be lost.")) }, confirmButton = { TextButton(onClick = close) { Text(strings("Discard changes")) } }, dismissButton = { TextButton(onClick = { discarding = false }) { Text(strings("Keep writing")) } })
    }
}

@Composable private fun StorageSection(model: JournalModel, chooseFolder: () -> Unit) {
    val strings = LocalStrings.current
    val guide = LocalGuide.current
    var confirmFolder by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings("Journal storage"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = Shell) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                Text(model.folderLabel ?: strings("App storage · this device only"), fontWeight = FontWeight.Bold)
                Text(if (model.folderUri == null) strings("Choose a local folder to keep your journal outside the app and sync it between devices.") else strings("Your swims are read and saved directly in this folder. Sync the entire folder with Syncthing, then select its local copy in FishyFishy on each device."), color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                model.lastChecked?.let { Text(strings("Folder last read at %s", it), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            }
        }
        Button(onClick = { confirmFolder = true }, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (model.folderUri == null) strings("Choose journal folder") else strings("Change or reconnect folder"))
        }
        OutlinedButton(onClick = { model.refresh() }, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text(strings("Refresh journal")) }
        Text(strings("Changes appear when you return to FishyFishy and while it’s open. Let Syncthing finish syncing before editing the same swim on another device. Folder sync is managed by Syncthing, not FishyFishy."), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        model.operationError?.let { Text(strings.error(it), color = MaterialTheme.colorScheme.error) }
        model.conflicts.forEach { (id, versions) ->
            Surface(color = Gold.copy(alpha = .2f), shape = Shell) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings("Two devices changed a swim"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(strings("Both versions are safe. Choose the one to keep. Past versions stay in the folder’s history."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    versions.forEachIndexed { index, revision ->
                        HorizontalDivider()
                        val trip = revision.trip
                        if (trip == null) Text(strings("Version %d: swim deleted", index + 1))
                        else {
                            Text(strings("Version %d: %s", index + 1, trip.place), fontWeight = FontWeight.Bold)
                            Text(strings("%s · %d min", LocalDate.parse(trip.date).format(DateTimeFormatter.ofPattern("d MMM yyyy", strings.locale)), trip.minutes))
                            Text(guide.filter { it.id in trip.sightings }.joinToString { it.display(strings.language) + if (it.id in trip.uncertain) " (?)" else "" }.ifBlank { strings("No creatures logged") }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (trip.notes.isNotEmpty()) Text(trip.notes)
                        }
                        OutlinedButton(onClick = { model.resolve(id, revision, versions.map { it.id }.toSet()) }, enabled = !model.busy && model.loadError == null) {
                            Text(if (trip == null) strings("Keep deletion") else strings("Keep version %d", index + 1))
                        }
                    }
                }
            }
        }
    }
    if (confirmFolder) AlertDialog(onDismissRequest = { confirmFolder = false },
        title = { Text(strings("Choose a folder for your journal")) },
        text = { Text(strings("Create a dedicated local folder such as Documents/FishyFishy. Your current swims and their history will be copied there, alongside any swims already in the folder. The original files stay untouched. Give Syncthing access to the same folder on each device.")) },
        confirmButton = { TextButton(onClick = { confirmFolder = false; chooseFolder() }) { Text(strings("Choose folder")) } },
        dismissButton = { TextButton(onClick = { confirmFolder = false }) { Text(strings("Cancel")) } })
}
