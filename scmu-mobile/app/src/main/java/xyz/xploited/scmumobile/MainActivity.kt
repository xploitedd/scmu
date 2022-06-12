package xyz.xploited.scmumobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import xyz.xploited.scmumobile.screen.CreateNavHost
import xyz.xploited.scmumobile.screen.bluetooth.BluetoothScreen
import xyz.xploited.scmumobile.screen.config.ConfigScreen
import xyz.xploited.scmumobile.screen.device.DeviceScreen
import xyz.xploited.scmumobile.screen.main.MainScreen
import xyz.xploited.scmumobile.ui.theme.SCMUMobileTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    Box(modifier = Modifier.padding(it)) {
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
    }
}