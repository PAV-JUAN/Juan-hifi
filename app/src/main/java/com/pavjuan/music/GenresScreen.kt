package com.pavjuan.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val BlueCrate = CrateLook(Color(0xFF3F6A96), Color(0xFF2A4F76), Color(0xFF12243A), Color(0xFFECF2F6), Shop.Tangerine, Shop.Navy)

val labelColors = listOf(
    Shop.Tangerine, Shop.SkimMilk, Shop.Butter, Color(0xFF9CC3E0),
    Color(0xFFD9603B), Color(0xFFC9A0C8), Color(0xFF7FC4B0), Color(0xFF8E949C)
)
val recordTilts = listOf(0f, 40f, 110f, 200f, 15f, 300f, 75f, 160f, 250f)

class GenrePick(val genre: Genre, val index: Int, val from: Offset, val sizePx: Int)

@Composable
fun GenresScreen(nav: NavController) {
    var genres by remember { mutableStateOf<List<Genre>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pick by remember { mutableStateOf<GenrePick?>(null) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    val open = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            genres = withContext(Dispatchers.IO) { fetchGenres() }
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun openGenre(p: GenrePick) {
        if (busy || pick != null) return
        busy = true
        pick = p
        scope.launch {
            try {
                open.snapTo(0f)
                open.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            } finally {
                busy = false
            }
        }
    }

    fun closeGenre() {
        if (busy || pick == null) return
        busy = true
        scope.launch {
            try {
                open.animateTo(0f, tween(650, easing = FastOutSlowInEasing))
            } finally {
                pick = null
                busy = false
            }
        }
    }

    BackHandler(enabled = pick != null) { closeGenre() }

    Box(Modifier.fillMaxSize().background(Shop.SkimMilk).onGloballyPositioned { rootPos = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            CrateHeader("GENRES", "${genres?.size ?: 0} RECORDS", BlueCrate) { nav.popBackStack() }

            if (error != null) Text("Couldn't open the crate: $error", color = Shop.StampRed, modifier = Modifier.padding(16.dp))
            if (genres?.isEmpty() == true) {
                Text("No genres yet. Your songs need genre tags first.", fontFamily = ShopFonts.Marker,
                    fontSize = 16.sp, color = Shop.Navy, modifier = Modifier.padding(24.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(genres ?: emptyList(), key = { _, g -> g.name }) { index, g ->
                    var pos by remember { mutableStateOf(Offset.Zero) }
                    var sizePx by remember { mutableStateOf(0) }
                    Column(
                        Modifier.clickable { openGenre(GenrePick(g, index, pos - rootPos, sizePx)) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.onGloballyPositioned { pos = it.positionInRoot(); sizePx = it.size.width }
                                .graphicsLayer { alpha = if (pick?.genre?.name == g.name) 0f else 1f }
                        ) {
                            Vinyl(100.dp, rotation = recordTilts[index % recordTilts.size], cover = null,
                                labelColor = labelColors[index % labelColors.size])
                        }
                        Text(g.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Shop.Navy,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                        Text("${g.songCount} SONGS", fontFamily = ShopFonts.Mono, fontSize = 10.sp, color = Color(0xFF4E5A6C))
                    }
                }
            }
        }

        pick?.let { GenreDetail(it, open.value) { closeGenre() } }
        MiniPlayer { nav.navigate("nowplaying") }
    }
}

@Composable
fun GenreDetail(pick: GenrePick, t: Float, onClose: () -> Unit) {
    val g = pick.genre
    var songs by remember(g.name) { mutableStateOf<List<Song>>(emptyList()) }
    var albums by remember(g.name) { mutableStateOf<List<Album>>(emptyList()) }
    LaunchedEffect(g.name) {
        try {
            songs = withContext(Dispatchers.IO) { fetchGenreSongs(g.name) }
            albums = withContext(Dispatchers.IO) { fetchGenreAlbums(g.name) }
        } catch (_: Exception) {
        }
    }

    val isOpen = t == 1f
    val spin = remember { Animatable(recordTilts[pick.index % recordTilts.size]) }
    LaunchedEffect(isOpen) {
        while (isOpen) spin.animateTo(spin.value + 360f, tween(2400, easing = LinearEasing))
    }

    val density = LocalDensity.current
    val size = with(density) { pick.sizePx.toDp() }
    val fromX = with(density) { pick.from.x.toDp() }
    val fromY = with(density) { pick.from.y.toDp() }
    val headT = ((t - 0.05f) / 0.6f).coerceIn(0f, 1f)
    val bodyT = ((t - 0.45f) / 0.55f).coerceIn(0f, 1f)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val toX = maxWidth - size - 20.dp
        val toY = 22.dp

        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(Shop.SkimMilk)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        )

        Box(
            Modifier.fillMaxWidth().height(140.dp)
                .graphicsLayer { translationY = -(1f - headT) * 160.dp.toPx() }
                .background(Brush.verticalGradient(listOf(BlueCrate.top, BlueCrate.bottom)))
        )

        Box(Modifier.offset(x = fromX + (toX - fromX) * t, y = fromY + (toY - fromY) * t)) {
            Vinyl(size, rotation = spin.value, cover = null, labelColor = labelColors[pick.index % labelColors.size])
        }

        Column(
            Modifier.fillMaxWidth().height(140.dp)
                .graphicsLayer { translationY = -(1f - headT) * 160.dp.toPx() }
                .padding(start = 6.dp, end = 16.dp, top = 14.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Text("←", fontSize = 24.sp, color = BlueCrate.ink) }
            Text(g.name.uppercase(), fontFamily = ShopFonts.Stamp, fontSize = 40.sp, color = BlueCrate.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp, end = size))
            Text("${g.songCount} SONGS · ${g.albumCount} ALBUMS", fontFamily = ShopFonts.Mono, fontSize = 11.sp,
                color = BlueCrate.ink.copy(alpha = 0.85f), modifier = Modifier.padding(start = 10.dp))
        }

        Column(
            Modifier.fillMaxSize().padding(top = 140.dp)
                .graphicsLayer { alpha = bodyT; translationY = (1f - bodyT) * 24.dp.toPx() }
        ) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Shop.Tangerine)
                        .clickable { Turntable.playSongs(songs) },
                    contentAlignment = Alignment.Center
                ) { Text("▶  Play ${g.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Shop.Navy,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.width(10.dp))
                RoundToggle("⤨", on = false) { Turntable.playSongs(songs.shuffled()) }
            }

            LazyColumn(
                Modifier.weight(1f).padding(top = 16.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                if (albums.isNotEmpty()) {
                    item {
                        Text("ALBUMS", fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                            color = Color(0xFF4E5A6C), modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(albums, key = { it.id }) { a ->
                                Column(Modifier.width(92.dp).clickable { Turntable.playAlbum(a.id) }) {
                                    Cover(a.coverArt, Modifier.size(92.dp))
                                    Text(a.name, fontSize = 11.sp, color = Shop.Navy, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
                item {
                    Text("SONGS", fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                        color = Color(0xFF4E5A6C), modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
                }
                itemsIndexed(songs, key = { _, s -> s.id }) { index, s ->
                    val now = Turntable.songId == s.id
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).clickable { Turntable.playSongs(songs, index) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(s.coverArt, Modifier.size(38.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(s.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                color = if (now) Shop.Tangerine else Shop.Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.artist, fontSize = 12.sp, color = Color(0xFF4E5A6C), maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        if (now) Text("▶", color = Shop.Tangerine, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}