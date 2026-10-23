package com.pavjuan.music

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

fun timeText(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun NowPlayingScreen(nav: NavController) {
    val playing = Turntable.wantsPlay

    val groupX by animateFloatAsState(
        if (playing) 0f else 63f,
        tween(700, delayMillis = if (playing) 0 else 1300, easing = FastOutSlowInEasing), label = "group"
    )
    val recordX by animateFloatAsState(
        if (playing) 130f else 0f,
        tween(900, delayMillis = if (playing) 600 else 500, easing = FastOutSlowInEasing), label = "record"
    )
    val arm by animateFloatAsState(
        if (playing) 15f else 0f,
        tween(700, delayMillis = if (playing) 1400 else 0, easing = FastOutSlowInEasing), label = "arm"
    )
    val spin = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        while (playing) spin.animateTo(spin.value + 360f, tween(1800, easing = LinearEasing))
    }
    val mineScale by animateFloatAsState(
        if (Turntable.albumStarred) 1f else 1.8f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "mineScale"
    )
    val mineAlpha by animateFloatAsState(if (Turntable.albumStarred) 1f else 0f, tween(150), label = "mineAlpha")

    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            pos = Turntable.positionMs()
            dur = Turntable.durationMs()
            delay(500)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Shop.Paper).verticalScroll(rememberScrollState()).padding(bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable { nav.popBackStack() },
                contentAlignment = Alignment.Center
            ) { Text("⌄", fontSize = 28.sp, color = Shop.Ink) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (playing) "NOW SPINNING" else "BACK IN THE SLEEVE", fontFamily = ShopFonts.Mono,
                    fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Shop.Ink)
                if (Turntable.album.isNotEmpty()) {
                    Text("from ${Turntable.album}", fontSize = 12.sp, color = Shop.Knot, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(48.dp))
        }

        if (!Turntable.hasTrack) {
            Text("Nothing on the turntable yet. Pick something on Home.",
                fontFamily = ShopFonts.Marker, fontSize = 18.sp, color = Shop.Knot,
                modifier = Modifier.padding(32.dp))
            return@Column
        }

        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            val s = maxWidth / 390.dp
            Box(Modifier.fillMaxWidth().height(280.dp * s)) {
                Box(
                    Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(390.dp, 280.dp)
                        .graphicsLayer { scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 0f) }
                ) {
                    Box(Modifier.offset(x = groupX.dp).size(390.dp, 280.dp)) {
                        Box(Modifier.offset(x = (20 + recordX).dp, y = 20.dp)) {
                            Vinyl(220.dp, rotation = spin.value, cover = Turntable.cover)
                        }
                        Box(
                            Modifier.offset(x = 14.dp, y = 10.dp).size(236.dp)
                                .clip(RoundedCornerShape(2.dp)).background(Shop.Board)
                        ) {
                            if (Turntable.cover.isNotEmpty()) {
                                AsyncImage(
                                    model = coverUrl(Turntable.cover, 500),
                                    contentDescription = "Album cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Box(
                            Modifier.offset(x = 196.dp, y = 2.dp).size(62.dp).rotate(12f)
                                .clip(CircleShape).background(Color(0xFFFBF5EA))
                                .clickable { Turntable.toggleAlbumStar() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("USED", fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold,
                                    fontSize = 7.sp, color = Shop.Knot)
                                Text("$38", fontFamily = ShopFonts.Marker, fontSize = 21.sp, color = Shop.StampRed)
                            }
                        }
                        Box(
                            Modifier.offset(x = 186.dp, y = 22.dp).size(82.dp, 22.dp)
                                .graphicsLayer {
                                    rotationZ = -14f
                                    scaleX = mineScale; scaleY = mineScale
                                    alpha = mineAlpha
                                }
                                .background(Color(0xFF2F5E4E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MINE", fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp, color = Shop.Paper)
                        }
                    }
                    Box(
                        Modifier.offset(x = 352.dp, y = 0.dp).size(22.dp).clip(CircleShape)
                            .background(Color(0xFFCFCBC2)).border(1.dp, Color(0xFF8C857A), CircleShape)
                    )
                    Box(
                        Modifier.offset(x = 360.dp, y = 11.dp).size(5.dp, 160.dp)
                            .graphicsLayer { rotationZ = arm; transformOrigin = TransformOrigin(0.5f, 0f) }
                    ) {
                        Box(
                            Modifier.size(5.dp, 150.dp).clip(RoundedCornerShape(3.dp)).background(
                                Brush.horizontalGradient(listOf(Color(0xFFA8A39A), Color(0xFFE8E5DF), Color(0xFFA8A39A)))
                            )
                        )
                        Box(Modifier.offset(x = (-5).dp, y = 140.dp).size(15.dp, 20.dp)
                            .clip(RoundedCornerShape(3.dp)).background(Color(0xFF2B2723)))
                    }
                }
            }
        }

        Turntable.error?.let {
            Text(
                "Couldn't save: $it", fontSize = 12.sp, color = Shop.Paper,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Shop.StampRed)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(Turntable.title, fontFamily = ShopFonts.Display, fontSize = 28.sp, color = Shop.Ink,
                    lineHeight = 30.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(Turntable.artist, fontSize = 15.sp, color = Shop.Knot, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable { Turntable.toggleSongStar() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (Turntable.songStarred) "♥" else "♡", fontSize = 26.sp, color = Shop.StampRed)
            }
        }

        val fraction = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
        Box(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp).fillMaxWidth().height(28.dp)
                .pointerInput(dur) {
                    detectTapGestures { tap ->
                        if (dur > 0) Turntable.seekTo((tap.x / size.width * dur).toLong())
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFD8CDB8)))
            Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Shop.Ink))
        }
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Text(timeText(pos), fontFamily = ShopFonts.Mono, fontSize = 11.sp, color = Shop.Knot)
            Spacer(Modifier.weight(1f))
            Text(timeText(dur), fontFamily = ShopFonts.Mono, fontSize = 11.sp, color = Shop.Knot)
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetalKey("|◀") { Turntable.previous() }
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(Shop.StampRed).clickable { Turntable.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (playing) "❚❚" else "▶", fontSize = 24.sp, color = Shop.Paper)
            }
            MetalKey("▶|") { Turntable.next() }
        }

        PaperCard(Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp).rotate(-0.6f).fillMaxWidth()) {
            Label("LINER NOTES")
            Text("About ${Turntable.album.ifEmpty { "this record" }}", fontFamily = ShopFonts.Display,
                fontSize = 17.sp, color = Shop.Ink)
            Text("The album's story from Wikipedia will go here: when it was recorded, the label, and what made it stand out.",
                fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Color(0xFF4A3A2E), lineHeight = 19.sp)
            Text("Read more · coming soon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Shop.Knot)
        }
    }
}

@Composable
fun MetalKey(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFD5D1C8))
            .border(1.dp, Color(0xFF8C857A), RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 16.sp, color = Shop.Ink, fontWeight = FontWeight.Bold)
    }
}