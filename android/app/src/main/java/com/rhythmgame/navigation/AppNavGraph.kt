package com.rhythmgame.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rhythmgame.ui.screens.*

private val enterTransition: EnterTransition = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
private val exitTransition: ExitTransition = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 4 }
private val popEnterTransition: EnterTransition = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
private val popExitTransition: ExitTransition = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {

        composable(
            NavRoutes.SPLASH,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(500)) },
        ) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(
            NavRoutes.HOME,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
        ) {
            HomeScreen(
                onUploadClick = { navController.navigate(NavRoutes.UPLOAD) },
                onSongClick = { songId -> navController.navigate(NavRoutes.songDetail(songId)) },
                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                onPlayClick = { navController.navigate(NavRoutes.SONG_LIST) },
                onRankedClick = { navController.navigate(NavRoutes.RANKED) },
                onProfileClick = { navController.navigate(NavRoutes.PROFILE) },
                onMultiplayerClick = { navController.navigate(NavRoutes.MULTIPLAYER) },
                onStoreClick = { navController.navigate(NavRoutes.STORE) },
            )
        }

        composable(
            NavRoutes.SONG_LIST,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            SongListScreen(
                onSongClick = { songId -> navController.navigate(NavRoutes.songDetail(songId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoutes.UPLOAD,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            UploadScreen(
                onUploadComplete = { songId ->
                    navController.navigate(NavRoutes.songDetail(songId)) {
                        popUpTo(NavRoutes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoutes.SETTINGS,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoutes.RANKED,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            RankedScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoutes.PROFILE,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoutes.MULTIPLAYER,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            MultiplayerScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            NavRoutes.STORE,
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            StoreScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = NavRoutes.SONG_DETAIL,
            arguments = listOf(navArgument("songId") { type = NavType.StringType }),
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: return@composable
            SongDetailScreen(
                songId = songId,
                onPlayClick = { difficulty ->
                    navController.navigate(NavRoutes.game(songId, difficulty))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = NavRoutes.GAME,
            arguments = listOf(
                navArgument("songId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
            ),
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(300)) },
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: return@composable
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: "medium"
            GameScreen(
                songId = songId,
                difficulty = difficulty,
                onGameFinished = { score, maxCombo, perfect, great, good, miss, overpress ->
                    navController.navigate(
                        NavRoutes.result(songId, difficulty, score, maxCombo, perfect, great, good, miss, overpress)
                    ) {
                        popUpTo(NavRoutes.songDetail(songId))
                    }
                },
                onBack = { navController.popBackStack() },
                onRestart = {
                    navController.navigate(NavRoutes.game(songId, difficulty)) {
                        popUpTo(NavRoutes.game(songId, difficulty)) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = NavRoutes.RESULT,
            arguments = listOf(
                navArgument("songId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
                navArgument("maxCombo") { type = NavType.IntType },
                navArgument("perfect") { type = NavType.IntType },
                navArgument("great") { type = NavType.IntType },
                navArgument("good") { type = NavType.IntType },
                navArgument("miss") { type = NavType.IntType },
                navArgument("overpress") { type = NavType.IntType; defaultValue = 0 },
            ),
            enterTransition = { fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.8f) },
            exitTransition = { fadeOut(tween(300)) },
            popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f) },
        ) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            val songId = args.getString("songId") ?: ""
            ResultScreen(
                songId = songId,
                difficulty = args.getString("difficulty") ?: "medium",
                score = args.getInt("score"),
                maxCombo = args.getInt("maxCombo"),
                perfect = args.getInt("perfect"),
                great = args.getInt("great"),
                good = args.getInt("good"),
                miss = args.getInt("miss"),
                overpress = args.getInt("overpress"),
                onPlayAgain = { selectedDifficulty ->
                    navController.navigate(NavRoutes.game(songId, selectedDifficulty)) {
                        popUpTo(NavRoutes.songDetail(songId))
                    }
                },
                onHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
