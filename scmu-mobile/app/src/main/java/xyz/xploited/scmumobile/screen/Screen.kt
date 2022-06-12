package xyz.xploited.scmumobile.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

data class Screen(
    val route: String,
    val navigation: NavGraphBuilder.(NavHostController, Screen) -> Unit
)

fun NavGraphBuilder.addScreen(navController: NavHostController, screen: Screen) {
    screen.navigation(this, navController, screen)
}

class ScreenBuilder {

    private var mainScreen: Screen? = null
    private val screenList: MutableList<Screen> = mutableListOf()

    fun setMainScreen(screen: Screen) {
        if (mainScreen != null)
            throw Exception("A main screen has already been set!")

        this.mainScreen = screen
        addScreen(screen)
    }

    fun addScreen(screen: Screen) {
        screenList.add(screen)
    }

    fun getMainScreen(): Screen {
        return mainScreen
            ?: (screenList.firstOrNull()
                ?: throw Exception("There should be at least one screen!"))
    }

    fun getScreens(): List<Screen> {
        return screenList
    }

}

@Composable
fun CreateNavHost(navController: NavHostController, screens: ScreenBuilder.() -> Unit) {
    val screenBuilder = ScreenBuilder()
    screens(screenBuilder)

    val mainScreen = screenBuilder.getMainScreen()
    val screensNav = screenBuilder.getScreens()

    NavHost(navController = navController, startDestination = mainScreen.route) {
        screensNav.forEach { this.addScreen(navController, it) }
    }
}