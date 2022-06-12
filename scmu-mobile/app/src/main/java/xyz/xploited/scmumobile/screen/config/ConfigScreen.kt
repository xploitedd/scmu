package xyz.xploited.scmumobile.screen.config

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.xploited.scmumobile.screen.Screen
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothViewModel
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScreen
import xyz.xploited.scmumobile.screen.bluetooth.WithBluetoothPermissions

val ConfigScreen = Screen(
    route = "config/{bluetoothAddress}",
    navigation = { navHost, screen ->
        composable(
            screen.route,
            arguments = listOf(
                navArgument("bluetoothAddress") { type = NavType.StringType }
            )
        ) {
            ConfigScreenView(
                bluetoothAddress = it.arguments?.getString("bluetoothAddress")!!,
                navigateToBluetooth = {
                    navHost.popBackStack(BluetoothScreen.route, inclusive = false)
                }
            )
        }
    }
)

@Composable
fun ConfigScreenView(
    bluetoothAddress: String,
    navigateToBluetooth: () -> Unit,
    btViewModel: BluetoothViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val context = LocalContext.current as ComponentActivity
    val blDevice = btViewModel.getBluetoothAdvertisement(bluetoothAddress)

    if (blDevice == null) {
        LaunchedEffect(Unit) {
            navigateToBluetooth()
        }
    } else {
        WithBluetoothPermissions {
            btViewModel.connect(blDevice)

            val wifiNetworks by btViewModel.getWifiNetworks()
                .observeAsState(initial = emptyList())

            LazyColumn {
                items(wifiNetworks) {
                    Text(text = it.ssid)
                }
            }
        }
    }
}

