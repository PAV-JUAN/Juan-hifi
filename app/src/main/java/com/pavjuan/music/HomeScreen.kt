package com.pavjuan.music

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random

data class Mood(val label: String, val greeting: String, val shelf: String, val note: String, val index: Int)

fun moodFor(hour: Int): Mood = when {
    hour < 5 -> Mood("LATE-NIGHT SHELF", "Still up, Juan?", "Late-night shelf", "AFTER 11 P.M.", 0)
    hour < 12 -> Mood("MORNING SHELF", "Morning, Juan.", "Morning shelf", "UNTIL NOON", 1)
    hour < 18 -> Mood("AFTERNOON SHELF", "Afternoon, Juan.", "Afternoon shelf", "NOON TO 6 P.M.", 2)
    hour < 23 -> Mood("EVENING SHELF", "Evening, Juan.", "Evening shelf", "6 TO 11 P.M.", 3)
    else -> Mood("LATE-NIGHT SHELF", "Evening, Juan.", "Late-night shelf", "AFTER 11 P.M.", 0)
}

class HomeData(val recent: List<Album>, val albumOfDay: Album?, val shelf: List<Album>, val fresh: List<Song>)

fun loadHome(moodIndex: Int): HomeData {
    val all = fetchAlbums("alphabeticalByName", 500)
    val recent = fetchAlbums("recent", 4)
    val day = (System.currentTimeMillis() / 86_400_000L).toInt()
    val albumOfDay = if (all.isEmpty()) null else all[day % all.size]
    val shelf = all.shuffled(Random(day * 10 + moodIndex)).take(3)
    val cutoff = daysAgoStamp(30)
    val fresh = fetchAllSongs()
        .filter { it.created.take(10) >= cutoff }
        .sortedByDescending { it.created }
        .take(3)
    return HomeData(recent, albumOfDay, shelf, fresh)
}

@Composable
fun HomeScreen(nav: NavController) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val mood = remember { moodFor(hour) }
    var data by remember { mutableStateOf<HomeData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val crate = remember { CrateState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            data = withContext(Dispatchers.IO) { loadHome(mood.index) }
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(Unit) {
        if (drawer.isOpen) drawer.snapTo(DrawerValue.Closed)
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ShopMenu { route ->
                when (route) {
                    null -> scope.launch { drawer.close() }
                    "logout" -> {
                        Turntable.forget()
                        Session.clear(context)
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }
                    else -> nav.navigate(route)
                }
            }
        }

    ) {
        Column(
            Modifier.fillMaxSize().background(Shop.Paper).verticalScroll(scroll).padding(bottom = 110.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                CoffeeOrTea(hour, Modifier.align(Alignment.TopEnd).offset(x = 30.dp, y = 30.dp))
                Column(Modifier.padding(start = 10.dp, end = 20.dp, top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Text("☰", fontSize = 24.sp, color = Shop.Ink)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "JUAN'S HI-FI · EST. 2026",
                            fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp, color = Shop.StampRed,
                            modifier = Modifier.rotate(-2f)
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.width(48.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(mood.label, fontFamily = ShopFonts.Mono, fontSize = 11.sp, color = Shop.Knot,
                        modifier = Modifier.padding(start = 10.dp))
                    Text(mood.greeting, fontFamily = ShopFonts.Display, fontSize = 36.sp, color = Shop.Ink,
                        modifier = Modifier.padding(start = 10.dp))
                }
            }

            if (error != null) {
                Text("Couldn't reach the shop: $error", color = Shop.StampRed,
                    modifier = Modifier.padding(20.dp))
            }

            SectionTitle("THIS WEEK'S BINS")
            BinCrate(crate) { i -> scope.launch { crate.pull(i) } }

            data?.let { d ->
                if (d.recent.isNotEmpty()) {
                    SectionTitle("Recently played")
                    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        d.recent.forEach { album ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(76.dp).clickable { Turntable.playAlbum(album.id) }
                            ) {
                                Vinyl(76.dp, rotation = scroll.value * 0.9f, cover = album.coverArt)
                                Text(album.name, fontSize = 11.sp, color = Shop.Knot, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }

                Row(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    d.albumOfDay?.let { a ->
                        PaperCard(Modifier.weight(1f).rotate(-1f)) {
                            Label("ALBUM OF THE DAY")
                            Cover(a.coverArt, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(a.name, fontFamily = ShopFonts.Display, fontSize = 16.sp, color = Shop.Ink)
                            Text(
                                if (a.year > 0) "${a.artist} · ${a.year}" else a.artist,
                                fontSize = 12.sp, color = Shop.Knot
                            )
                            Text("put this on tonight →", fontFamily = ShopFonts.Marker, fontSize = 13.sp,
                                color = Shop.StampRed,
                                modifier = Modifier.rotate(-2f).clickable { Turntable.playAlbum(a.id) }
                                    .padding(vertical = 6.dp))
                        }
                    }
                    if (d.fresh.isNotEmpty()) {
                        PaperCard(Modifier.weight(1f).rotate(0.8f).padding(top = 10.dp)) {
                            Label("FRESH FROM THE CONVERTER")
                            d.fresh.forEachIndexed { index, s ->
                                Column(Modifier.fillMaxWidth().clickable { Turntable.playSongs(d.fresh, index) }) {
                                    Text(s.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Shop.Ink,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.artist, fontSize = 12.sp, color = Shop.Knot, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                            Text("Clears 30 days after your last new song", fontSize = 11.sp, color = Shop.Knot)
                        }
                    }
                }

                if (d.shelf.isNotEmpty()) {
                    SectionTitle(mood.shelf)
                    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        d.shelf.forEach { a ->
                            Column(Modifier.width(104.dp).clickable { Turntable.playAlbum(a.id) }) {
                                Cover(a.coverArt, Modifier.size(104.dp))
                                Text(a.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Shop.Ink,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
        MiniPlayer { nav.navigate("nowplaying") }
        BinOverlay(crate)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, fontFamily = ShopFonts.Display, fontSize = 22.sp, color = Shop.Ink,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 12.dp)
    )
}

@Composable
fun Label(text: String) {
    Text(text, fontFamily = ShopFonts.Mono, fontSize = 10.sp, color = Shop.Knot)
}

@Composable
fun Cover(coverArt: String, modifier: Modifier) {
    AsyncImage(
        model = coverUrl(coverArt),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(2.dp)).background(Shop.Board)
    )
}

@Composable
fun PaperCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFBF5EA)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun ShopMenu(onPick: (String?) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = Shop.Paper, modifier = Modifier.width(290.dp)) {
        WoodBoard(Modifier.fillMaxWidth().height(96.dp), seed = 11) {
            Column(Modifier.padding(start = 20.dp, top = 22.dp)) {
                Text("JUAN'S HI-FI", fontFamily = ShopFonts.Mono, fontSize = 10.sp, color = Shop.Ink)
                Text("The shop floor", fontFamily = ShopFonts.Display, fontSize = 24.sp, color = Shop.Ink)
            }
            Text(
                "19", fontFamily = ShopFonts.Stamp, fontSize = 36.sp,
                color = Shop.StampRed.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).rotate(-7f)
            )
        }
        Spacer(Modifier.height(8.dp))
        MenuRow("Home") { onPick(null) }
        MenuRow("Albums") { onPick("albums") }
        MenuRow("Genres") { onPick("genres") }
        MenuRow("Radio") { onPick("radio") }
        MenuRow("Artists", soon = true) {}
        MenuRow("Search", soon = true) {}
        MenuRow("Favorites", soon = true) {}
        Spacer(Modifier.weight(1f))
        MenuRow("Leave the shop") { onPick("logout") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun MenuRow(label: String, soon: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp)
            .clickable(enabled = !soon, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = ShopFonts.Body, fontSize = 16.sp,
            color = if (soon) Shop.Knot else Shop.Ink)
        Spacer(Modifier.weight(1f))
        if (soon) Text("soon", fontFamily = ShopFonts.Marker, fontSize = 12.sp, color = Shop.Knot)
    }
}