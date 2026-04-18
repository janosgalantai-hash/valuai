package com.valuai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object Estimation : Screen("estimation")
    object History    : Screen("history")
    object Profile    : Screen("profile")
    object Result     : Screen("result/{estimationId}") {
        fun createRoute(id: String) = "result/$id"
    }
    object Login    : Screen("login")
    object Register : Screen("register")
    object Info     : Screen("info")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)
val bottomNavItems = listOf(
    BottomNavItem(Screen.Estimation, "Appraise", Icons.Default.Star),
    BottomNavItem(Screen.History,    "History",  Icons.Default.History),
    BottomNavItem(Screen.Profile,    "Profile",  Icons.Default.Person),
    BottomNavItem(Screen.Info,       "Info",     Icons.Default.Info),
)