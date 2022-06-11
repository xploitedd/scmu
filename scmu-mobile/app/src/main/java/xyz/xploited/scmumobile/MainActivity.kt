package xyz.xploited.scmumobile

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.navigation.compose.rememberNavController
import xyz.xploited.scmumobile.screen.CreateNavHost
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScreen
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScannerViewModel
import xyz.xploited.scmumobile.screen.config.ConfigScreen
import xyz.xploited.scmumobile.screen.device.DeviceScreen
import xyz.xploited.scmumobile.screen.main.MainScreen
import xyz.xploited.scmumobile.ui.theme.SCMUMobileTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val bluetoothViewModel: BluetoothScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val bluetoothIntentFilter = IntentFilter()
//        bluetoothIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
//        bluetoothIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        registerReceiver(bluetoothReceiver, bluetoothIntentFilter)
//
//        val application = application as MobileApplication
//        val bluetoothAdapter = application.bluetoothAdapter
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val scanPermission = ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_SCAN
//            )
//
//            when(scanPermission) {
//                PackageManager.PERMISSION_DENIED -> {
//                    // TODO
//                    Log.e("POCRL", "No permission damn")
//                }
//                else -> {
//                    bluetoothViewModel.searching.observe(this) {
//                        if (it && !bluetoothAdapter.isDiscovering) {
//                            bluetoothAdapter.startDiscovery()
//                        } else if (!it && bluetoothAdapter.isDiscovering) {
//                            bluetoothAdapter.cancelDiscovery()
//                        }
//                    }
//                }
//            }
//        }

        setContent {
            SCMUMobileTheme {
                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = { Text(text = "SCMU Mobile") },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) {
                    val navController = rememberNavController()
                    CreateNavHost(navController = navController) {
                        setMainScreen(MainScreen)
                        addScreen(DeviceScreen)
                        addScreen(BluetoothScreen)
                        addScreen(ConfigScreen)
                    }
                }
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        bluetoothViewModel.addBluetoothDevice(device)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    bluetoothViewModel.resetBluetoothSearch()
                }
                else -> {}
            }
        }

    }
}