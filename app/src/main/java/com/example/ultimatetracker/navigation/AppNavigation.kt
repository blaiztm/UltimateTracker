package com.example.ultimatetracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.example.ultimatetracker.ui.screens.DetailScreen
import com.example.ultimatetracker.ui.screens.EditScreen
import com.example.ultimatetracker.ui.screens.HomeScreen
import com.example.ultimatetracker.ui.screens.SettingsScreen
import com.example.ultimatetracker.ui.screens.BrowseScreen
import com.example.ultimatetracker.ui.screens.AccountScreen
import com.example.ultimatetracker.ui.screens.AuthScreen
import com.example.ultimatetracker.ui.screens.StatisticsScreen
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.AccountViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator

private const val ITEM_ID = "itemId"

@Composable
fun AppNavigation(viewModel: MediaViewModel, accountViewModel: AccountViewModel) {
    val accountState by accountViewModel.state.collectAsStateWithLifecycle()
    if (!accountState.initialized) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (accountState.user == null) {
        AuthScreen(accountViewModel)
        return
    }
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel,
                onAdd = { navController.navigate("edit/0") },
                onOpen = { navController.navigate("detail/$it") },
                onSettings = { navController.navigate("settings") },
                onStatistics = { navController.navigate("statistics") },
                onBrowse = { query -> navController.navigate("browse?query=${Uri.encode(query)}") },
                onAccount = {
                    navController.navigate(if (accountState.user?.isGuest == true) "auth" else "account")
                },
            )
        }
        composable("auth") {
            if (accountState.user?.isGuest == true) {
                AuthScreen(accountViewModel, navController::popBackStack)
            } else {
                AccountScreen(accountViewModel, navController::popBackStack)
            }
        }
        composable("account") { AccountScreen(accountViewModel, navController::popBackStack) }
        composable("settings") { SettingsScreen(viewModel, navController::popBackStack) }
        composable("statistics") { StatisticsScreen(viewModel, navController::popBackStack) }
        composable("browse?query={query}", arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })) { entry ->
            BrowseScreen(viewModel, entry.arguments?.getString("query").orEmpty(), navController::popBackStack) {
                navController.navigate("edit/0")
            }
        }
        composable("detail/{$ITEM_ID}", arguments = listOf(navArgument(ITEM_ID) { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong(ITEM_ID) ?: return@composable
            DetailScreen(viewModel, id, onBack = navController::popBackStack, onEdit = { navController.navigate("edit/$id") })
        }
        composable("edit/{$ITEM_ID}", arguments = listOf(navArgument(ITEM_ID) { type = NavType.LongType })) { entry ->
            EditScreen(viewModel, entry.arguments?.getLong(ITEM_ID) ?: 0, onBack = navController::popBackStack)
        }
    }
}
