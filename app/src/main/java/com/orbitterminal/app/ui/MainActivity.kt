package com.orbitterminal.app.ui

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.*
import com.orbitterminal.app.data.agent.AntigravityLoginHandler
import com.orbitterminal.app.data.datastore.AppSettings
import com.orbitterminal.app.ui.chat.ChatScreen
import com.orbitterminal.app.ui.navigation.Screen
import com.orbitterminal.app.ui.terminal.TerminalScreen
import com.orbitterminal.app.ui.agents.AgentsScreen
import com.orbitterminal.app.ui.settings.SettingsScreen
import com.orbitterminal.app.ui.theme.OrbitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var antigravityLoginHandler: AntigravityLoginHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        setContent {
            val themeMode by appSettings.themeMode.collectAsState(initial = "dark")
            val dynamicColor by appSettings.dynamicColor.collectAsState(initial = true)
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> true
            }

            OrbitTheme(darkTheme = isDark, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OrbitMainApp(onExit = { showExitDialog() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "orbitterminal" && uri.host == "auth") {
            val token = uri.getQueryParameter("token") ?: uri.getQueryParameter("code")
            if (!token.isNullOrEmpty()) {
                val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                scope.launch {
                    antigravityLoginHandler.submitAuthToken(token)
                }
            }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Orbit Terminal AI?")
            .setMessage("Do you want to exit Orbit Terminal AI?")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

data class NavItem(val label: String, val route: String, val icon: ImageVector)

@Composable
fun OrbitMainApp(onExit: () -> Unit) {
    val navController = rememberNavController()
    val navItems = listOf(
        NavItem("Chats", Screen.Chat.route, Icons.Default.Chat),
        NavItem("Terminal", Screen.Terminal.route, Icons.Default.Terminal),
        NavItem("Agents", Screen.Agents.route, Icons.Default.SmartToy),
        NavItem("Settings", Screen.Settings.route, Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Terminal.route) { TerminalScreen() }
            composable(Screen.Agents.route) { AgentsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
