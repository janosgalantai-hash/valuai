package com.valuai

import com.valuai.network.CameraRepository
import com.valuai.screens.LoginScreen
import com.valuai.screens.CameraScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest

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
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppEventBus.unauthorized.collectLatest {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Bottom nav csak a fő képernyőkön látszik (Info nem saját route, ezért kizárjuk)
    val showBottomBar = bottomNavItems
        .filter { it.screen != Screen.Info }
        .any { it.screen.route == currentRoute }

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
                        val selected = item.screen != Screen.Info && currentRoute == item.screen.route
                        val label = when (item.screen) {
                            Screen.Estimation -> strings.navAppraise
                            Screen.History    -> strings.navHistory
                            Screen.Profile    -> strings.navProfile
                            Screen.Info       -> strings.navInfo
                            else              -> item.label
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (item.screen == Screen.Info) {
                                    showInfoDialog = true
                                } else {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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

    if (showInfoDialog) {
        val infoText = when (currentRoute) {
            Screen.Estimation.route -> strings.infoEstimation
            Screen.History.route    -> strings.infoHistory
            Screen.Profile.route    -> strings.infoProfile
            else                    -> strings.infoEstimation
        }
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInfoDialog = false },
                colors = CardDefaults.cardColors(containerColor = com.valuai.ui.theme.SurfaceDark),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = strings.infoTitle,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = com.valuai.ui.theme.GoldPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = infoText,
                        fontSize = 15.sp,
                        color = androidx.compose.ui.graphics.Color.White,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.infoClose,
                        fontSize = 12.sp,
                        color = com.valuai.ui.theme.TextSecondary
                    )
                }
            }
        }
    }
}