package com.chessassistant.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chessassistant.featureboard.BoardScreen
import com.chessassistant.featureboard.BoardViewModel
import com.chessassistant.featuregames.GamesScreen
import com.chessassistant.featuresettings.SettingsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

private sealed class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    data object Board : TopLevel("board", "Board", Icons.Filled.Analytics)
    data object Games : TopLevel("games", "Games", Icons.Filled.Games)
    data object Settings : TopLevel("settings", "Settings", Icons.Filled.Settings)
}

private val tabs = listOf(TopLevel.Board, TopLevel.Games, TopLevel.Settings)

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
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
            startDestination = TopLevel.Board.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopLevel.Board.route) {
                val vm: BoardViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                BoardScreen(
                    state = state,
                    onSquareClick = vm::onSquareClick,
                    onNewGame = vm::newGame,
                    onUndo = vm::undo,
                    onRedo = vm::redo,
                    onFlip = vm::flip,
                    onSave = vm::saveGame,
                )
            }
            composable(TopLevel.Games.route) {
                GamesScreen(onGameClick = { _ -> })
            }
            composable(TopLevel.Settings.route) {
                SettingsScreen()
            }
        }
    }
}