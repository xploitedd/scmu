package xyz.xploited.scmumobile.screen.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.juul.kable.Advertisement
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
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    btViewModel: BluetoothViewModel = viewModel(LocalContext.current as ComponentActivity),
    navigateToConfig: (String) -> Unit
) {
    WithBluetoothPermissions {
        var devices by remember { mutableStateOf(emptyList<Advertisement>()) }
        DisposableEffect(lifecycleOwner) {
            val liveData = btViewModel.startSearch()

            liveData.observe(lifecycleOwner) {
                devices = it
            }

            onDispose {
                liveData.removeObservers(lifecycleOwner)
                btViewModel.stopSearch()
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
                        title = it.name!!,
                        onClick = { navigateToConfig(it.address) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WithBluetoothPermissions(
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