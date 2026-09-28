package com.erosketarakoa.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.erosketarakoa.app.ui.about.AboutScreen
import com.erosketarakoa.app.ui.detail.ListDetailScreen
import com.erosketarakoa.app.ui.lists.ListsScreen
import com.erosketarakoa.app.ui.settings.SettingsScreen

/** Top-level navigation destinations. */
object Routes {
    const val LISTS = "lists"
    const val LIST_DETAIL = "lists/{listId}"
    const val ABOUT = "about"
    const val SETTINGS = "settings"

    fun listDetail(listId: String) = "lists/$listId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.LISTS) {
        composable(Routes.LISTS) {
            ListsScreen(
                onOpenList = { listId -> navController.navigate(Routes.listDetail(listId)) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.LIST_DETAIL,
            arguments = listOf(navArgument("listId") { type = NavType.StringType }),
        ) {
            ListDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
