package xyz.xploited.scmumobile.screen.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import xyz.xploited.scmumobile.database.entities.Device
import xyz.xploited.scmumobile.screen.Screen
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScreen
import xyz.xploited.scmumobile.screen.device.DeviceViewModel
import xyz.xploited.scmumobile.ui.common.Card

val MainScreen = Screen(
    route = "main",
    navigation = { navHost, screen ->
        composable(screen.route) {
            MainScreenView(
                navigateToBluetoothScreen = { navHost.navigate(BluetoothScreen.route) },
                navigateToDevice = { navHost.navigate("device/$it") }
            )
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenView(
    deviceViewModel: DeviceViewModel = viewModel(LocalContext.current as ComponentActivity),
    navigateToBluetoothScreen: () -> Unit,
    navigateToDevice: (Int) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navigateToBluetoothScreen() }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new device"
                )
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            val devices: List<Device> by deviceViewModel
                .devicesList
                .observeAsState(initial = emptyList())

            DevicesList(devices, navigateToDevice)
        }
    }
}

@Composable
private fun DevicesList(
    devicesList: List<Device>,
    navigateToDevice: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item { Text(text = "My Devices", fontWeight = FontWeight.Bold, fontSize = 22.sp) }
        if (devicesList.isEmpty()) {
            item { Text(text = "No devices found. Try adding one :)") }
        } else {
            items(devicesList) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Device ${it.deviceName}",
                    subtitle = it.macAddress,
                    onClick = { navigateToDevice(it.id) }
                )
            }
        }
    }
}