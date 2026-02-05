package com.fukudai.meshiroulette.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fukudai.meshiroulette.presentation.detail.RestaurantDetailScreen
import com.fukudai.meshiroulette.presentation.list.RestaurantListScreen
import com.fukudai.meshiroulette.presentation.roulette.RouletteScreen

sealed class Screen(val route: String) {
    data object Roulette : Screen("roulette")
    data object List : Screen("list")
    data object Detail : Screen("detail/{restaurantId}") {
        fun createRoute(restaurantId: String) = "detail/$restaurantId"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Roulette : BottomNavItem(
        route = Screen.Roulette.route,
        title = "ルーレット",
        icon = Icons.Default.Casino
    )
    data object List : BottomNavItem(
        route = Screen.List.route,
        title = "一覧",
        icon = Icons.Default.List
    )
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(BottomNavItem.Roulette, BottomNavItem.List)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = bottomNavItems.any { item ->
                currentDestination?.hierarchy?.any { it.route == item.route } == true
            }

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.title)
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Roulette.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Roulette.route) {
                RouletteScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }
            composable(Screen.List.route) {
                RestaurantListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("restaurantId") { type = NavType.StringType }
                )
            ) {
                RestaurantDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
