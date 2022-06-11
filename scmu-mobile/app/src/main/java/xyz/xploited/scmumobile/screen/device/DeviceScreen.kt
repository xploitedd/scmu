package xyz.xploited.scmumobile.screen.device

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.xploited.scmumobile.screen.Screen

val DeviceScreen = Screen(
    route = "device/{deviceId}",
    navigation = { _, screen ->
        composable(
            screen.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) {
            DeviceScreenView()
        }
    }
)

@Composable
fun DeviceScreenView() {

}