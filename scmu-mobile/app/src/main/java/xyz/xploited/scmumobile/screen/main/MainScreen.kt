package xyz.xploited.scmumobile.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.composable
import xyz.xploited.scmumobile.screen.Screen
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScreen
import xyz.xploited.scmumobile.ui.common.Card

val MainScreen = Screen(
    route = "main",
    navigation = { navHost, screen ->
        composable(screen.route) {
            MainScreenView(
                navigateToBluetoothScreen = { navHost.navigate(BluetoothScreen.route) }
            )
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenView(
    navigateToBluetoothScreen: () -> Unit
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
        AddedDevicesList(0)
    }
}

@Composable
private fun AddedDevicesList(deviceCount: Int = 0) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item { Text(text = "My Devices", fontWeight = FontWeight.Bold, fontSize = 22.sp) }
        if (deviceCount == 0) {
            item { Text(text = "No devices found. Try adding one :)") }
        } else {
            items(deviceCount) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Device $it"
                )
            }
        }
    }
}