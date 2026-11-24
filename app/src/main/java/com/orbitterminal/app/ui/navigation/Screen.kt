package com.orbitterminal.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object Chat : Screen("chat")
    object Terminal : Screen("terminal")
    object Agents : Screen("agents")
    object Settings : Screen("settings")
    object GitHub : Screen("github")
}
