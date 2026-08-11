package com.chessassistant.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chessassistant.featureboard.BoardScreen
import com.chessassistant.featureboard.BoardViewModel
import com.chessassistant.featuregames.GamesScreen
import com.chessassistant.featuresettings.SettingsScreen
import com.chessassistant.featureassistant.AssistantScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

private object Routes {
    const val BOARD = "board"
    const val BOARD_GAME = "board?gameId={gameId}"
    const val ASSISTANT = "assistant"
    const val GAMES = "games"
    const val SETTINGS = "settings"
}

private sealed class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    data object Board : TopLevel(Routes.BOARD, "Board", Icons.Filled.Analytics)
    data object Assistant : TopLevel(Routes.ASSISTANT, "Asisten", Icons.Filled.SmartToy)
    data object Games : TopLevel(Routes.GAMES, "Games", Icons.Filled.Games)
    data object Settings : TopLevel(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
}

private const val GAME_ID_ARG = "gameId"

private val tabs = listOf(TopLevel.Assistant, TopLevel.Board, TopLevel.Games, TopLevel.Settings)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = currentRoute?.substringBefore('?')

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevel.Assistant.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                route = Routes.BOARD_GAME,
                arguments = listOf(
                    navArgument(GAME_ID_ARG) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val vm: BoardViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                val gameId = entry.arguments?.getLong(GAME_ID_ARG) ?: -1L
                LaunchedEffect(gameId) {
                    if (gameId >= 0) vm.loadGame(gameId)
                }
                BoardScreen(
                    state = state,
                    onSquareClick = vm::onSquareClick,
                    onNewGame = vm::newGame,
                    onUndo = vm::undo,
                    onRedo = vm::redo,
                    onFlip = vm::flip,
                    onSave = vm::saveGame,
                    onAiModeChange = vm::setAiMode,
                )
            }
            composable(TopLevel.Games.route) {
                GamesScreen(onGameClick = { id ->
                    navController.navigate("${Routes.BOARD}?$GAME_ID_ARG=$id") {
                        launchSingleTop = true
                    }
                })
            }
            composable(TopLevel.Assistant.route) {
                AssistantScreen()
            }
            composable(TopLevel.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
