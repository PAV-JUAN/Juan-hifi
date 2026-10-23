package com.pavjuan.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.load(this)
        Turntable.init(this)
        setContent { ShopApp() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) Turntable.release()
    }
}

@Composable
fun ShopApp() {
    val nav = rememberNavController()
    val start = remember { if (Session.isLoggedIn) "home" else "login" }

    NavHost(navController = nav, startDestination = start) {

        composable(
            "login",
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) }
        ) { LoginScreen(nav) }

        composable(
            "home",
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
            popEnterTransition = { EnterTransition.None }
        ) { HomeScreen(nav) }

        composable(
            "nowplaying",
            enterTransition = { slideInVertically(tween(550)) { it } },
            popExitTransition = { slideOutVertically(tween(550)) { it } }
        ) { NowPlayingScreen(nav) }

        composable(
            "albums",
            enterTransition = { slideInHorizontally(tween(450)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(450)) { -it } }
        ) { AlbumsScreen(nav) }

        composable(
            "genres",
            enterTransition = { slideInHorizontally(tween(450)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(450)) { -it } }
        ) { GenresScreen(nav) }

        composable(
            "radio",
            enterTransition = { slideInHorizontally(tween(450)) { -it } },
            popExitTransition = { slideOutHorizontally(tween(450)) { -it } }
        ) { PlaceholderScreen("COMING SOON", Shop.Paper, Shop.WallPaint, nav) }
    }
}

@Composable
fun PlaceholderScreen(title: String, background: Color, ink: Color, nav: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().background(background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontFamily = ShopFonts.Paint, fontSize = 52.sp, color = ink)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { nav.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = ink)
        ) { Text("Go back", fontFamily = ShopFonts.Body, color = background) }
    }
}