package com.example.ultimatetracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ultimatetracker.ui.screens.DetailScreen
import com.example.ultimatetracker.ui.screens.EditScreen
import com.example.ultimatetracker.ui.screens.HomeScreen
import com.example.ultimatetracker.viewmodel.MediaViewModel

private const val ITEM_ID = "itemId"

@Composable
fun AppNavigation(viewModel: MediaViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel, onAdd = { navController.navigate("edit/0") }, onOpen = { navController.navigate("detail/$it") })
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
