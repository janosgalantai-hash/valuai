package com.valuai

import com.valuai.network.CameraRepository
import com.valuai.screens.LoginScreen
import com.valuai.screens.CameraScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.valuai.i18n.LocalStrings
import com.valuai.i18n.stringsForLanguage
import com.valuai.ui.theme.BackgroundDark
import com.valuai.ui.theme.GoldPrimary
import com.valuai.ui.theme.TextSecondary
import com.valuai.ui.theme.ValuAITheme
import com.valuai.screens.EstimationScreen
import com.valuai.screens.HistoryScreen
import com.valuai.screens.ProfileScreen
import com.valuai.screens.RegisterScreen
import com.valuai.screens.ResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ValuAITheme {
                ValuAIApp()
            }
        }
    }
}

@Composable
fun ValuAIApp() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val language by tokenManager.language.collectAsState(initial = "English")
    val strings = stringsForLanguage(language)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom nav csak a 3 fő képernyőn látszik
    val showBottomBar = bottomNavItems.any { it.screen.route == currentRoute }

    CompositionLocalProvider(LocalStrings provides strings) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFF12121E),
                    tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        val label = when (item.screen) {
                            Screen.Estimation -> strings.navAppraise
                            Screen.History    -> strings.navHistory
                            Screen.Profile    -> strings.navProfile
                            else              -> item.label
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoldPrimary,
                                selectedTextColor = GoldPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Color(0xFF1E1E30)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Camera.route) {
                CameraScreen(
                    navController = navController,
                    onImageCaptured = { uri ->
                        navController.popBackStack(Screen.Estimation.route, false)
                    }
                )
            }
            composable(Screen.Login.route) { LoginScreen(navController) }
            composable(Screen.Register.route) { RegisterScreen(navController) }
            composable(Screen.Estimation.route) { EstimationScreen(navController) }
            composable(Screen.History.route) { HistoryScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Result.route) { backStack ->
                val id = backStack.arguments?.getString("estimationId") ?: ""
                ResultScreen(estimationId = id, navController = navController)
            }
        }
    }
    } // CompositionLocalProvider
}