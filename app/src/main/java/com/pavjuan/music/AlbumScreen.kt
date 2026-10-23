package com.pavjuan.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrateLook(val top: Color, val bottom: Color, val hole: Color, val ink: Color, val chip: Color, val chipInk: Color)

val RedCrate = CrateLook(Color(0xFFB4473A), Color(0xFF933428), Color(0xFF3A1712), Color(0xFFFBEFE2), Shop.Butter, Shop.Ink)

@Composable
fun CrateHoles(look: CrateLook, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(8) {
            Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(3.dp)).background(look.hole))
        }
    }
}

@Composable
fun CrateHeader(title: String, count: String, look: CrateLook, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(look.top, look.bottom)))
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.offset(x = (-10).dp).size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("←", fontSize = 24.sp, color = look.ink) }
            Spacer(Modifier.weight(1f))
            Text("CRATE No.19 · DO NOT REMOVE", fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp, color = look.ink.copy(alpha = 0.85f))
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(34.dp))
        }
        CrateHoles(look, Modifier.padding(top = 8.dp))
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
            Text(title, fontFamily = ShopFonts.Stamp, fontSize = 40.sp, color = look.ink.copy(alpha = 0.92f))
            Spacer(Modifier.weight(1f))
            Text(
                count, fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = look.chipInk,
                modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(look.chip)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }
}

class AlbumPick(val album: Album, val from: Offset, val sizePx: Int)

@Composable
fun AlbumsScreen(nav: NavController) {
    var albums by remember { mutableStateOf<List<Album>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var byArtist by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<AlbumPick?>(null) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }
    val open = remember { Animatable(0f) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            albums = withContext(Dispatchers.IO) { fetchAlbums("alphabeticalByName", 500) }
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun openAlbum(p: AlbumPick) {
        if (busy || pick != null) return
        busy = true
        pick = p
        scope.launch {
            try {
                open.snapTo(0f)
                open.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            } finally {
                busy = false
            }
        }
    }

    fun closeAlbum() {
        if (busy || pick == null) return
        busy = true
        scope.launch {
            try {
                open.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            } finally {
                pick = null
                busy = false
            }
        }
    }

    BackHandler(enabled = pick != null) { closeAlbum() }

    val shown = remember(albums, query, byArtist) {
        val list = albums ?: emptyList()
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) list else list.filter {
            it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
        }
        if (byArtist) filtered.sortedWith(compareBy({ it.artist.lowercase() }, { it.name.lowercase() }))
        else filtered.sortedBy { it.name.lowercase() }
    }

    Box(Modifier.fillMaxSize().background(Shop.Milk).onGloballyPositioned { rootPos = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize()) {
            CrateHeader("ALBUMS", "${albums?.size ?: 0} RECORDS", RedCrate) { nav.popBackStack() }

            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFFFFAF1))
                        .border(1.dp, Color(0xFFD8C7AC), RoundedCornerShape(22.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) Text("Find an album or artist", fontSize = 15.sp, color = Shop.Knot)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = ShopFonts.Body, fontSize = 15.sp, color = Shop.Ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).border(1.dp, Shop.Ink, RoundedCornerShape(22.dp))
                        .clickable { byArtist = !byArtist }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (byArtist) "ARTIST" else "A–Z", fontFamily = ShopFonts.Mono,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Shop.Ink)
                }
            }

            if (error != null) Text("Couldn't open the crate: $error", color = Shop.StampRed, modifier = Modifier.padding(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shown, key = { it.id }) { a ->
                    var pos by remember { mutableStateOf(Offset.Zero) }
                    var sizePx by remember { mutableStateOf(0) }
                    Column(Modifier.clickable { openAlbum(AlbumPick(a, pos - rootPos, sizePx)) }) {
                        Cover(
                            a.coverArt,
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .onGloballyPositioned { pos = it.positionInRoot(); sizePx = it.size.width }
                                .graphicsLayer { alpha = if (pick?.album?.id == a.id) 0f else 1f }
                        )
                        Text(a.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Shop.Ink, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        Text(a.artist, fontSize = 12.sp, color = Shop.Knot, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        pick?.let { AlbumDetail(it, open.value) { closeAlbum() } }
        MiniPlayer { nav.navigate("nowplaying") }
    }
}

@Composable
fun AlbumDetail(pick: AlbumPick, t: Float, onClose: () -> Unit) {
    val a = pick.album
    var songs by remember(a.id) { mutableStateOf<List<Song>>(emptyList()) }
    LaunchedEffect(a.id) {
        try {
            songs = withContext(Dispatchers.IO) { fetchAlbumSongs(a.id) }
        } catch (_: Exception) {
        }
    }

    val density = LocalDensity.current
    val size = with(density) { pick.sizePx.toDp() }
    val fromX = with(density) { pick.from.x.toDp() }
    val fromY = with(density) { pick.from.y.toDp() }
    val toX = 16.dp
    val toY = 88.dp
    val headT = ((t - 0.1f) / 0.6f).coerceIn(0f, 1f)
    val bodyT = ((t - 0.45f) / 0.55f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(Shop.Milk)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        )

        Box(
            Modifier.fillMaxWidth().height(152.dp)
                .graphicsLayer { translationY = -(1f - headT) * size.toPx().coerceAtLeast(152.dp.toPx()) }
                .background(Brush.verticalGradient(listOf(RedCrate.top, RedCrate.bottom)))
        ) {
            Box(
                Modifier.padding(start = 6.dp, top = 14.dp).size(44.dp).clip(CircleShape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Text("←", fontSize = 24.sp, color = RedCrate.ink) }
            CrateHoles(RedCrate, Modifier.padding(start = 16.dp, end = 16.dp, top = 64.dp))
        }

        Column(
            Modifier.fillMaxSize().padding(top = toY)
                .graphicsLayer { alpha = bodyT; translationY = (1f - bodyT) * 24.dp.toPx() }
        ) {
            Column(
                Modifier.padding(start = toX + size + 14.dp, end = 16.dp).height(size),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(a.name, fontFamily = ShopFonts.Display, fontSize = 22.sp, lineHeight = 24.sp, color = Shop.Ink,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(a.artist, fontSize = 14.sp, color = Color(0xFF4A3C2E), modifier = Modifier.padding(top = 3.dp))
                Text(
                    (if (a.year > 0) "${a.year} · " else "") + "${songs.size} SONGS",
                    fontFamily = ShopFonts.Mono, fontSize = 11.sp, color = Shop.Knot, modifier = Modifier.padding(top = 3.dp)
                )
            }

            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(RedCrate.bottom)
                        .clickable { Turntable.playSongs(songs) },
                    contentAlignment = Alignment.Center
                ) { Text("▶  Play album", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RedCrate.ink) }
                Spacer(Modifier.width(10.dp))
                RoundToggle("⟲", on = Turntable.repeatAll) { Turntable.setRepeat(!Turntable.repeatAll) }
                Spacer(Modifier.width(10.dp))
                RoundToggle("⤨", on = false) { Turntable.playSongs(songs.shuffled()) }
            }
            Text(
                if (Turntable.repeatAll) "REPEAT ON · STARTS OVER AFTER THE LAST SONG" else "TAP A SONG TO START THE ALBUM FROM THERE",
                fontFamily = ShopFonts.Mono, fontSize = 10.sp, color = Shop.Knot,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )

            LazyColumn(
                Modifier.weight(1f).padding(top = 6.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp)
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, s ->
                    val now = Turntable.songId == s.id
                    Row(
                        Modifier.fillMaxWidth().height(50.dp).clickable { Turntable.playSongs(songs, index) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (now) "▶" else "%02d".format(index + 1), fontFamily = ShopFonts.Mono, fontSize = 12.sp,
                            color = if (now) RedCrate.bottom else Shop.Knot, modifier = Modifier.width(30.dp))
                        Text(s.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = if (now) RedCrate.bottom else Shop.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Text(timeText(s.duration * 1000L), fontFamily = ShopFonts.Mono, fontSize = 12.sp, color = Shop.Knot)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD8C7AC)))
                }
            }
        }

        Cover(
            a.coverArt,
            Modifier.offset(x = fromX + (toX - fromX) * t, y = fromY + (toY - fromY) * t)
                .size(size)
                .graphicsLayer { rotationZ = -1.5f * t }
        )
    }
}

@Composable
fun RoundToggle(symbol: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(50.dp).clip(CircleShape).background(if (on) Shop.Ink else Color.Transparent)
            .border(1.dp, Shop.Ink, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 20.sp, color = if (on) Shop.Milk else Shop.Ink)
    }
}