package com.pavjuan.music

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    val sway by rememberInfiniteTransition(label = "sign").animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )

    fun stepInside() {
        if (working) return
        if (server.isBlank() || user.isBlank() || password.isEmpty()) {
            problem = "Fill in all three lines of the guest book first."
            return
        }
        working = true
        problem = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) { logIn(context, server, user, password) }
                nav.navigate("home") { popUpTo("login") { inclusive = true } }
            } catch (e: Exception) {
                problem = e.message ?: "The door won't open. Check the server address."
            } finally {
                working = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Shop.Paper).verticalScroll(rememberScrollState()).imePadding()
    ) {
        Row(Modifier.fillMaxWidth().height(46.dp)) {
            repeat(9) { i ->
                Box(Modifier.weight(1f).fillMaxHeight().background(if (i % 2 == 0) Shop.StampRed else Shop.Paper))
            }
        }
        Row(Modifier.fillMaxWidth().offset(y = (-9).dp)) {
            repeat(9) { i ->
                Box(Modifier.weight(1f).height(18.dp).clip(CircleShape)
                    .background(if (i % 2 == 0) Shop.StampRed else Color(0xFFE9DFCB)))
            }
        }

        WoodBoard(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(92.dp).clip(RoundedCornerShape(4.dp)), seed = 31) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JUAN'S HI-FI", fontFamily = ShopFonts.Stamp, fontSize = 34.sp, color = Shop.Ink)
                Text("RECORDS · EST. 2026", fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp, color = Shop.StampRed)
            }
        }

        Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.width(170.dp).height(190.dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(Color(0xFF2E4A3A)).padding(14.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF9FB8B0).copy(alpha = 0.55f))
                        .border(3.dp, Color(0xFF223629), RoundedCornerShape(3.dp))
                )
                Box(Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-22).dp).size(10.dp)
                    .clip(CircleShape).background(Shop.Butter))
            }
            Column(
                Modifier.padding(top = 30.dp)
                    .graphicsLayer { rotationZ = sway; transformOrigin = TransformOrigin(0.5f, 0f) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(46.dp)) {
                    Box(Modifier.width(1.dp).height(22.dp).background(Shop.Ink))
                    Box(Modifier.width(1.dp).height(22.dp).background(Shop.Ink))
                }
                Text(
                    "OPEN", fontFamily = ShopFonts.Stamp, fontSize = 20.sp, color = Shop.StampRed,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFFFBF5EA))
                        .border(2.dp, Shop.StampRed, RoundedCornerShape(3.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(8.dp).background(Color(0xFF5B5249)))

        PaperCard(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp).rotate(-0.8f).fillMaxWidth()) {
            Text("Sign the guest book", fontFamily = ShopFonts.Display, fontSize = 22.sp, color = Shop.Ink)
            Text("Your Navidrome details. Only a login token is kept on this phone, never your password.",
                fontSize = 12.sp, color = Shop.Knot, lineHeight = 17.sp)
            GuestLine("SERVER", server, "music.ricamivida.com", KeyboardType.Uri) { server = it }
            GuestLine("NAME", user, "your Navidrome username", KeyboardType.Text) { user = it }
            GuestLine(
                "PASSWORD", password, "••••••••", KeyboardType.Password,
                hidden = !showPassword,
                trailing = {
                    Text(if (showPassword) "hide" else "show", fontFamily = ShopFonts.Marker, fontSize = 13.sp,
                        color = Shop.Knot, modifier = Modifier.clickable { showPassword = !showPassword }.padding(6.dp))
                }
            ) { password = it }

            problem?.let {
                Text(it, fontFamily = ShopFonts.Marker, fontSize = 14.sp, color = Shop.StampRed,
                    modifier = Modifier.padding(top = 4.dp).rotate(-1f))
            }

            Box(
                Modifier.padding(top = 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                    .background(if (working) Shop.Knot else Shop.StampRed).clickable { stepInside() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (working) "Unlocking the door…" else "Step inside", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = Shop.Paper)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun GuestLine(
    label: String,
    value: String,
    hint: String,
    keyboard: KeyboardType,
    hidden: Boolean = false,
    trailing: @Composable () -> Unit = {},
    onChange: (String) -> Unit
) {
    Column(Modifier.padding(top = 10.dp)) {
        Text(label, fontFamily = ShopFonts.Mono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = Shop.Knot)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                if (value.isEmpty()) Text(hint, fontSize = 16.sp, color = Color(0xFFB9A98E))
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                    textStyle = TextStyle(fontFamily = ShopFonts.Body, fontSize = 16.sp, color = Shop.Ink),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCDBB9A)))
    }
}