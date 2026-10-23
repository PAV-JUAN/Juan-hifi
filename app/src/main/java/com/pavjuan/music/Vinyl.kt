package com.pavjuan.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.random.Random

@Composable
fun Vinyl(size: Dp, rotation: Float, cover: String?, labelColor: Color = Shop.StampRed) {
    Box(Modifier.size(size).rotate(rotation), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2
            drawCircle(Color(0xFF15100C), radius)
            var r = radius - 3.dp.toPx()
            while (r > radius * 0.4f) {
                drawCircle(Color(0xFF2B221B), r, style = Stroke(width = 1f))
                r -= 3.dp.toPx()
            }
        }
        val label = Modifier.size(size * 0.36f).clip(CircleShape).background(labelColor)
        if (!cover.isNullOrEmpty()) {
            AsyncImage(
                model = coverUrl(cover, 150),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = label
            )
        } else {
            Box(label)
        }
        Box(Modifier.size(size * 0.05f).clip(CircleShape).background(Color(0xFF15100C)))
    }
}

@Composable
fun WoodBoard(
    modifier: Modifier = Modifier,
    seed: Int = 1,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier.drawBehind {
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color(0xFFC9BDA3), Color(0xFFD3C8AF), Color(0xFFC2B497), Color(0xFFCDC1A7))
                )
            )
            val rnd = Random(seed)
            repeat(28) {
                val y = rnd.nextFloat() * size.height
                val drift = (rnd.nextFloat() - 0.5f) * 8f
                val thick = if (rnd.nextInt(5) == 0) 2.2f else 1f
                drawLine(Color(0x245C4A32), Offset(0f, y), Offset(size.width, y + drift), strokeWidth = thick)
            }
            if (rnd.nextInt(3) == 0) {
                drawOval(
                    Color(0x40503E28),
                    topLeft = Offset(size.width * rnd.nextFloat() * 0.8f, size.height * 0.35f),
                    size = Size(40.dp.toPx(), 8.dp.toPx())
                )
            }
            drawRect(Brush.verticalGradient(listOf(Color(0x26FFFFFF), Color(0x00000000), Color(0x2A000000))))
        },
        content = content
    )
}

@Composable
fun CoffeeOrTea(hour: Int, modifier: Modifier = Modifier) {
    val coffee = if (hour in 12..17) 1f - (hour - 12) / 6f else 0f
    val tea = hour >= 18
    Canvas(modifier.size(130.dp)) {
        val brown = Color(0xFF7A481C)
        if (coffee > 0f) {
            drawCircle(
                brown.copy(alpha = 0.16f * coffee),
                radius = size.minDimension / 2 - 6.dp.toPx(),
                style = Stroke(8.dp.toPx())
            )
            drawCircle(
                brown.copy(alpha = 0.14f * coffee),
                radius = 7.dp.toPx(),
                center = Offset(14.dp.toPx(), size.height - 10.dp.toPx())
            )
        }
        if (tea) {
            drawOval(
                Color(0xFF96622A).copy(alpha = 0.16f),
                topLeft = Offset(30.dp.toPx(), 40.dp.toPx()),
                size = Size(74.dp.toPx(), 80.dp.toPx())
            )
            drawRoundRect(
                Color(0xFFD9C29A),
                topLeft = Offset(44.dp.toPx(), 50.dp.toPx()),
                size = Size(46.dp.toPx(), 58.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                Color(0x596E461E),
                topLeft = Offset(52.dp.toPx(), 58.dp.toPx()),
                size = Size(30.dp.toPx(), 42.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawLine(
                Color(0xFF8C7A62),
                Offset(67.dp.toPx(), 50.dp.toPx()),
                Offset(105.dp.toPx(), 18.dp.toPx()),
                strokeWidth = 1.5f
            )
        }
    }
}