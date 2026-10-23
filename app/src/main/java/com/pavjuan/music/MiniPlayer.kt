package com.pavjuan.music

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Turntable.isPlaying) {
        while (Turntable.isPlaying) {
            angle.animateTo(angle.value + 360f, tween(1800, easing = LinearEasing))
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) {
            Turntable.error?.let {
                Text(
                    "Couldn't play: $it", fontSize = 12.sp, color = Shop.Paper,
                    modifier = Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(6.dp))
                        .background(Shop.StampRed).padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            if (Turntable.hasTrack) {
                WoodBoard(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(10.dp)),
                    seed = 21
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Vinyl(40.dp, rotation = angle.value, cover = Turntable.cover)
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clickable(onClick = onOpen)
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(Turntable.title, fontFamily = ShopFonts.Body, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = Shop.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(Turntable.artist, fontFamily = ShopFonts.Body, fontSize = 12.sp,
                                color = Shop.Knot, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(Shop.StampRed)
                                .clickable { Turntable.toggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (Turntable.isPlaying) "❚❚" else "▶", color = Shop.Paper, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}