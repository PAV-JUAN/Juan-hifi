package com.pavjuan.music

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath

const val SLEEVE = 280f
const val FRONT_BOARD_TOP = 170f
const val HANDOFF = 0.62f

data class Bin(
    val name: String, val tag: String, val color: Color, val ink: Color,
    val top: Int, val nudge: Int, val songs: List<Pair<String, String>>
)

val weekBins = listOf(
    Bin("Hip-Hop Mix", "HIP-HOP", Color(0xFFE2B44A), Shop.Ink, 12, 8, listOf(
        "Corner Store" to "Big Laval", "Rewind" to "Mic Tide", "Static" to "Northside Kid",
        "Dusty Loops" to "Crate Theory", "Night Bus" to "Mic Tide")),
    Bin("Latin Mix", "LATIN", Color(0xFF2F5E4E), Shop.Paper, 62, -14, listOf(
        "Tarde de Domingo" to "Los Hermanos Cruz", "Agua Clara" to "Sol Mestizo", "Malecón" to "Orquesta Alba",
        "Ventana" to "Lucía Duarte", "Calle Ocho" to "Los Hermanos Cruz")),
    Bin("Soul Mix", "SOUL", Color(0xFFB8501A), Shop.Paper, 112, 16, listOf(
        "Les Nuits Longues" to "Marée Basse", "Northbound" to "Kay Rivers", "Velvet Hour" to "The Tidewater Five",
        "Sunday Again" to "Odette Lane", "Second Story" to "June & the Keys"))
)

fun dipFor(j: Int, selected: Int?): Float =
    if (selected != null && j > selected) (FRONT_BOARD_TOP - weekBins[j].top + 4f).coerceAtLeast(0f) else 0f

fun liftNeeded(i: Int): Float = weekBins[i].top + SLEEVE - FRONT_BOARD_TOP + 6f

fun hideLine(i: Int, liftPart: Float): Float {
    var line = FRONT_BOARD_TOP
    for (j in i + 1 until weekBins.size) {
        val top = weekBins[j].top + dipFor(j, i) * liftPart
        if (top < line) line = top
    }
    return line
}

object OpenTopClip : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, -10_000f, size.width, size.height))
}

class CrateState {
    var selected by mutableStateOf<Int?>(null)
    var playing by mutableStateOf(false)
    var crateInRoot by mutableStateOf(Offset.Zero)
    var overlayInRoot by mutableStateOf(Offset.Zero)
    val slotInRoot = mutableStateMapOf<Int, Offset>()
    var busy = false
    val progress = Animatable(0f)
    val flip = Animatable(0f)
    val recordOut = Animatable(0f)
    val spin = Animatable(0f)

    val liftPart: Float get() = (progress.value / HANDOFF).coerceIn(0f, 1f)
    val travelPart: Float get() = ((progress.value - HANDOFF) / (1f - HANDOFF)).coerceIn(0f, 1f)

    suspend fun pull(i: Int) {
        if (busy || selected != null) return
        busy = true
        try {
            progress.snapTo(0f); flip.snapTo(0f); recordOut.snapTo(0f)
            selected = i
            progress.animateTo(1f, tween(1250, easing = FastOutSlowInEasing))
        } finally {
            busy = false
        }
    }

    suspend fun putBack() {
        if (busy || selected == null) return
        busy = true
        try {
            playing = false
            coroutineScope {
                if (recordOut.value > 0f) launch { recordOut.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
                if (flip.value > 0f) launch { flip.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
            }
            progress.animateTo(0f, tween(1150, easing = FastOutSlowInEasing))
        } finally {
            progress.snapTo(0f)
            selected = null
            busy = false
        }
    }

    suspend fun flipOver() {
        flip.animateTo(if (flip.value < 90f) 180f else 0f, tween(800, easing = FastOutSlowInEasing))
    }

    suspend fun togglePlay() {
        if (!playing) {
            playing = true
            recordOut.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        } else {
            playing = false
            recordOut.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
        }
    }
}

@Composable
fun SleeveFront(b: Bin, modifier: Modifier = Modifier) {
    Box(modifier.size(SLEEVE.dp).clip(RoundedCornerShape(2.dp)).background(b.color)) {
        Box(
            Modifier.offset(x = 142.dp, y = 60.dp).size(190.dp).clip(CircleShape)
                .background(Shop.Paper.copy(alpha = 0.9f))
        )
        Text(b.name, fontFamily = ShopFonts.Display, fontSize = 21.sp, color = b.ink,
            modifier = Modifier.padding(start = 14.dp, top = 9.dp))
        Text(
            b.tag, fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Shop.Ink,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp)
                .background(Color(0xFFF6E9C4)).padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SleeveBack(b: Bin, modifier: Modifier = Modifier) {
    Column(
        modifier.size(SLEEVE.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFF6EEDD))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(b.name, fontFamily = ShopFonts.Display, fontSize = 20.sp, color = Shop.Ink)
            Spacer(Modifier.weight(1f))
            Text("SIDE A", fontFamily = ShopFonts.Mono, fontSize = 9.sp, color = Shop.Knot)
        }
        Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(3.dp).background(b.color))
        b.songs.forEachIndexed { n, (title, artist) ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("0${n + 1}", fontFamily = ShopFonts.Mono, fontSize = 10.sp, color = Shop.Knot,
                    modifier = Modifier.width(22.dp).padding(top = 2.dp))
                Text("$title · $artist", fontSize = 13.sp, color = Shop.Ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Text("+ 20 more in the bin", fontSize = 11.sp, color = Shop.Knot, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.weight(1f))
        Text("GENERATED EVERY MONDAY · 3 A.M. EASTERN", fontFamily = ShopFonts.Mono, fontSize = 8.sp, color = Shop.Knot)
    }
}

@Composable
fun BinCrate(state: CrateState, onPull: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(232.dp)
            .onGloballyPositioned { state.crateInRoot = it.positionInRoot() }
            .clip(OpenTopClip)
    ) {
        val center = (maxWidth - SLEEVE.dp) / 2
        Box(Modifier.matchParentSize().clip(RoundedCornerShape(3.dp)).background(Color(0xFF3B2F22)))
        weekBins.forEachIndexed { i, b ->
            SleeveFront(
                b,
                Modifier.offset(x = center + b.nudge.dp, y = (b.top + dipFor(i, state.selected) * state.liftPart).dp)
                    .onGloballyPositioned { state.slotInRoot[i] = it.positionInRoot() }
                    .graphicsLayer { alpha = if (state.selected == i) 0f else 1f }
                    .clickable(enabled = state.selected == null) { onPull(i) }
            )
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFF2A2118))) {
            WoodBoard(Modifier.fillMaxWidth().height(30.dp), seed = 3)
            Spacer(Modifier.height(4.dp))
            WoodBoard(Modifier.fillMaxWidth().height(28.dp), seed = 7)
        }
        Text(
            "No.19", fontFamily = ShopFonts.Stamp, fontSize = 44.sp, color = Shop.StampRed.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 4.dp).rotate(-5f)
        )
        Text(
            "PACKED FOR RECORDS\n↑ THIS SIDE UP", fontFamily = ShopFonts.Mono, fontSize = 9.sp,
            color = Shop.Ink.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 22.dp)
        )
    }
}

@Composable
fun BinOverlay(state: CrateState) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val i = state.selected ?: return
    val b = weekBins[i]

    LaunchedEffect(state.playing) {
        while (state.playing) {
            state.spin.animateTo(state.spin.value + 360f, tween(1800, easing = LinearEasing))
        }
    }

    val t = state.travelPart
    val lift = liftNeeded(i) * state.liftPart
    val out = state.recordOut.value
    val flip = state.flip.value
    val slot = state.slotInRoot[i] ?: Offset.Zero

    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { state.overlayInRoot = it.positionInRoot() }) {
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(Color(0x9E1E140C))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    scope.launch { state.putBack() }
                }
        )

        val slotX = with(density) { (slot.x - state.overlayInRoot.x).toDp() }
        val slotY = with(density) { (slot.y - state.overlayInRoot.y).toDp() }
        val startY = slotY - lift.dp
        val liftedY = slotY - liftNeeded(i).dp
        val endX = (maxWidth - SLEEVE.dp) / 2
        val endY = liftedY.coerceIn(16.dp, 90.dp)

        val hidePath = remember { Path() }
        Box(
            Modifier.fillMaxSize().drawWithContent {
                if (t == 0f) {
                    val crateTop = state.crateInRoot.y - state.overlayInRoot.y
                    val sleevePx = SLEEVE.dp.toPx()
                    hidePath.reset()
                    hidePath.addRect(Rect(-100_000f, crateTop + FRONT_BOARD_TOP.dp.toPx(), 100_000f, 100_000f))
                    for (j in i + 1 until weekBins.size) {
                        val s = state.slotInRoot[j] ?: continue
                        val left = s.x - state.overlayInRoot.x
                        val top = crateTop + (weekBins[j].top + dipFor(j, i) * state.liftPart).dp.toPx()
                        hidePath.addRect(Rect(left, top, left + sleevePx, top + sleevePx))
                    }
                    clipPath(hidePath, clipOp = ClipOp.Difference) {
                        this@drawWithContent.drawContent()
                    }
                } else {
                    drawContent()
                }
            }
        ) {
            Box(
                Modifier.offset(x = slotX + (endX - slotX) * t, y = startY + (endY - startY) * t)
                    .size(SLEEVE.dp).graphicsLayer { rotationZ = -2f * t }
            ) {
                Box(Modifier.offset(x = (-48 * out).dp).size(SLEEVE.dp)) {
                    Box(Modifier.offset(x = (15 + 96 * out).dp, y = 15.dp)) {
                        Vinyl(250.dp, rotation = state.spin.value, cover = null, labelColor = b.color)
                    }
                    Box(
                        Modifier.size(SLEEVE.dp)
                            .graphicsLayer { rotationY = flip; cameraDistance = 14f * density.density }
                            .clickable(enabled = t == 1f) { scope.launch { state.flipOver() } }
                    ) {
                        if (flip <= 90f) SleeveFront(b) else SleeveBack(b, Modifier.graphicsLayer { rotationY = 180f })
                    }
                }
            }
        }

        Column(
            Modifier.offset(y = endY + 310.dp).fillMaxWidth().padding(horizontal = 30.dp)
                .graphicsLayer { alpha = ((t - 0.8f) / 0.2f).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (flip > 90f) "the back of the sleeve" else "tap the sleeve to flip it",
                fontFamily = ShopFonts.Marker, fontSize = 14.sp, color = Shop.Paper)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { scope.launch { state.flipOver() } },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text(if (flip > 90f) "Front ↻" else "Flip ↻", color = Shop.Paper, fontFamily = ShopFonts.Body) }
                Button(
                    onClick = {
                        if (state.playing) Turntable.pause() else Turntable.playShuffle()
                        scope.launch { state.togglePlay() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Shop.StampRed),
                    modifier = Modifier.weight(1.4f).height(50.dp)
                ) { Text(if (state.playing) "Spinning · pause" else "Play the bin", color = Shop.Paper, fontFamily = ShopFonts.Body) }
            }
            TextButton(onClick = { scope.launch { state.putBack() } }) {
                Text("Put it back in the bin", color = Shop.Paper, fontFamily = ShopFonts.Body)
            }
        }
    }
}