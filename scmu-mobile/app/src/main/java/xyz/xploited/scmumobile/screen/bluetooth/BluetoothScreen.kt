package xyz.xploited.scmumobile.screen.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import xyz.xploited.scmumobile.MobileApplication
import xyz.xploited.scmumobile.screen.Screen
import xyz.xploited.scmumobile.ui.common.Card
import xyz.xploited.scmumobile.ui.common.Container

val BluetoothScreen = Screen(
    "bluetooth",
    navigation = { navHost, screen ->
        composable(screen.route) {
            BluetoothScreenView(
                navigateToConfig = { navHost.navigate("config/$it") }
            )
        }
    }
)

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreenView(
    btScannerViewModel: BluetoothScannerViewModel = viewModel(LocalContext.current as ComponentActivity),
    navigateToConfig: (String) -> Unit
) {
    val context = LocalContext.current as ComponentActivity
    val application = context.application as MobileApplication
    val blAdapter = application.bluetoothAdapter

    WithBluetoothPermissions {
        RegisterBluetoothReceiver(context, btScannerViewModel)

        val searching: Boolean by btScannerViewModel.searching
            .observeAsState(initial = false)

        val devices: Set<BluetoothDevice> by btScannerViewModel.bluetoothDevices
            .observeAsState(initial = emptySet())

        LaunchedEffect(searching) {
            Log.d("BluetoothScreen", "searching: $searching, isDiscovering: ${blAdapter.isDiscovering}")
            if (searching && !blAdapter.isDiscovering) {
                Log.d("BluetoothScreen", "starting discovery")
                blAdapter.startDiscovery()
            } else if (!searching && blAdapter.isDiscovering) {
                Log.d("BluetoothScreen", "stopping discovery")
                blAdapter.cancelDiscovery()
            }
        }

        DisposableEffect(Unit) {
            btScannerViewModel.startSearch()

            onDispose {
                btScannerViewModel.stopSearch()
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { Text(text = "Bluetooth Devices", fontWeight = FontWeight.Bold, fontSize = 22.sp) }

            val filteredDevices = devices.filter { it.name != null }
            if (filteredDevices.isEmpty()) {
                item { Text(text = "Searching for bluetooth devices...") }
            } else {
                items(filteredDevices) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        title = it.name,
                        onClick = { navigateToConfig(it.address) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun WithBluetoothPermissions(
    onPermissions: @Composable () -> Unit
) {
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(permissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    } else {
        rememberMultiplePermissionsState(permissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    if (bluetoothPermissions.allPermissionsGranted) {
        onPermissions()
    } else {
        Container(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "The application needs to access your location in order to connect to bluetooth devices",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = { bluetoothPermissions.launchMultiplePermissionRequest() }) {
                    Text(text = "Grant Permissions")
                }
            }
        }

        LaunchedEffect(bluetoothPermissions) {
            bluetoothPermissions.launchMultiplePermissionRequest()
        }
    }
}

@Composable
private fun RegisterBluetoothReceiver(
    context: Context,
    btScannerViewModel: BluetoothScannerViewModel
) {
    DisposableEffect(context, btScannerViewModel) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) {
                            Log.d("BluetoothScreen", "New bluetooth device ${device.address}")
                            btScannerViewModel.addBluetoothDevice(device)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> btScannerViewModel.resetBluetoothSearch()
                }
            }
        }

        context.registerReceiver(broadcast, intentFilter)
        Log.d("BluetoothScreen", "registered")

        onDispose {
            context.unregisterReceiver(broadcast)
            Log.d("BluetoothScreen", "unregistered")
        }
    }
}