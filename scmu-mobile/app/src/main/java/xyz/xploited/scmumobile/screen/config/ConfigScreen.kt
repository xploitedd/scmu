package xyz.xploited.scmumobile.screen.config

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCallback
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.xploited.scmumobile.screen.Screen
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScannerViewModel
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScreen

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
                    navHost.popBackStack()
                }
            )
        }
    }
)

@SuppressLint("MissingPermission")
@Composable
fun ConfigScreenView(
    bluetoothAddress: String,
    navigateToBluetooth: () -> Unit,
    btScannerViewModel: BluetoothScannerViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val context = LocalContext.current as ComponentActivity
    val blDevice = btScannerViewModel.getBluetoothDeviceByAddress(bluetoothAddress)

    if (blDevice == null) {
        LaunchedEffect(Unit) {
            navigateToBluetooth()
        }
    } else {
        blDevice.connectGatt(context, true, object : BluetoothGattCallback() {

        })
    }
}