package com.pavjuan.music

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

object Shop {
    // Old fruit crate: Home, bins, side menu
    val Board = Color(0xFFCDC1A7)
    val Paper = Color(0xFFF1E8D6)
    val StampRed = Color(0xFFB03A2A)
    val Knot = Color(0xFF8A7458)
    val Ink = Color(0xFF2E261E)
    // Red milk crate: Albums
    val CrateRed = Color(0xFFA43C30)
    val Milk = Color(0xFFF2E9DA)
    val Butter = Color(0xFFE6B44A)
    // Blue milk crate: Genres
    val CrateBlue = Color(0xFF345D88)
    val SkimMilk = Color(0xFFECEEE6)
    val Tangerine = Color(0xFFE08A3C)
    val Navy = Color(0xFF1A2230)
    // Radio wall paint
    val WallPaint = Color(0xFF233A5A)
}

object ShopFonts {
    val Display = FontFamily(Font(R.font.fraunces_extrabold, FontWeight.ExtraBold))
    val Body = FontFamily(
        Font(R.font.dmsans_regular, FontWeight.Normal),
        Font(R.font.dmsans_bold, FontWeight.Bold)
    )
    val Mono = FontFamily(
        Font(R.font.plexmono_regular, FontWeight.Normal),
        Font(R.font.plexmono_semibold, FontWeight.SemiBold)
    )
    val Stamp = FontFamily(Font(R.font.alfaslab_regular))
    val Marker = FontFamily(Font(R.font.permanentmarker_regular))
    val Paint = FontFamily(Font(R.font.knewave_regular))
}