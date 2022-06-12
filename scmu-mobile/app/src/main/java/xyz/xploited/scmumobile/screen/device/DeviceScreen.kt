package xyz.xploited.scmumobile.screen.device

import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import xyz.xploited.scmumobile.common.StateResult
import xyz.xploited.scmumobile.database.entities.Device
import xyz.xploited.scmumobile.screen.Screen
import xyz.xploited.scmumobile.screen.main.MainScreen
import xyz.xploited.scmumobile.ui.common.Card
import xyz.xploited.scmumobile.ui.common.Container
import java.time.Instant
import kotlin.random.Random

@Parcelize
private data class Timestamped<T : Parcelable>(
    val value: T,
    val instant: Instant = Instant.now()
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Timestamped<*>

        if (value != other.value) return false
        if (instant != other.instant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + instant.hashCode()
        return result
    }
}

val DeviceScreen = Screen(
    route = "device/{deviceId}",
    navigation = { navHost, screen ->
        composable(
            screen.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.IntType }
            )
        ) {
            DeviceScreenView(
                deviceId = it.arguments!!.getInt("deviceId"),
                popUpToMain = {
                    navHost.popBackStack(route = MainScreen.route, inclusive = false)
                }
            )
        }
    }
)

@Composable
fun DeviceScreenView(
    deviceId: Int,
    deviceViewModel: DeviceViewModel = viewModel(LocalContext.current as ComponentActivity),
    popUpToMain: () -> Unit = {}
) {
    val currentPopUpToMain by rememberUpdatedState(popUpToMain)
    val deviceRes by fetchDeviceById(deviceViewModel, deviceId)

    if (deviceRes.isLoading) {
        Container {
            Text(text = "Loading Device...", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    } else if (deviceRes.isError) {
        LaunchedEffect(deviceRes) {
            currentPopUpToMain()
        }
    } else {
        val device = (deviceRes as StateResult.Success<Device>).value
        DeviceDetails(
            deviceViewModel = deviceViewModel,
            device = device,
            popUpToMain = popUpToMain
        )
    }
}

@Composable
private fun fetchDeviceById(
    deviceViewModel: DeviceViewModel,
    deviceId: Int
): State<StateResult<Device>> {
    return produceState(initialValue = StateResult.loading(), deviceId) {
        val device = deviceViewModel.getDeviceById(deviceId)
        value = if (device == null) {
            StateResult.error()
        } else {
            StateResult.success(device)
        }
    }
}

@Composable
private fun DeviceDetails(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    deviceViewModel: DeviceViewModel,
    device: Device,
    popUpToMain: () -> Unit
) {
    var webSocketData: Timestamped<DeviceIncomingData>? by rememberSaveable { mutableStateOf(null) }

    DisposableEffect(lifecycleOwner) {
        val liveData = deviceViewModel.getWebsocketData(device)

        liveData.observe(lifecycleOwner) {
            if (it.isSuccess) {
                webSocketData = Timestamped((it as StateResult.Success<DeviceIncomingData>).value)
            } else if (it.isError) {
                val error = (it as StateResult.Error<DeviceIncomingData>).error
                Log.e("DeviceScreen", "An error occurred while consuming from the websocket", error)
                // popUpToMain()
            }
        }

        onDispose {
            webSocketData = null
            liveData.removeObservers(lifecycleOwner)
            deviceViewModel.closeWebsocket()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Container {
            Column {
                Text(text = "Device: ${device.deviceName}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = device.macAddress, fontSize = 12.sp)
            }
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Container {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DisplaySection(
                            isClosed = webSocketData?.value?.isClosed,
                            isRaining = webSocketData?.value?.isRaining,
                            pm25Level = webSocketData?.value?.pm25Level,
                            pm10Level = webSocketData?.value?.pm10Level
                        )

                        if (webSocketData != null) {
                            ConfigurationSection(
                                thresholds = Timestamped(webSocketData!!.value.thresholds),
                                updateThresholds = {
                                    deviceViewModel.updateThresholds(it)
                                }
                            )
                        }

                        if (scrollState.maxValue > 0 && scrollState.maxValue < Int.MAX_VALUE) {
                            SettingsSection(
                                deviceViewModel = deviceViewModel,
                                device = device,
                                popUpToMain = popUpToMain
                            )
                        }
                    }
                }
            }

            if (scrollState.maxValue == 0) {
                Column {
                    SettingsSection(
                        deviceViewModel = deviceViewModel,
                        device = device,
                        popUpToMain = popUpToMain
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplaySection(
    isClosed: Boolean?,
    isRaining: Boolean?,
    pm25Level: Int?,
    pm10Level: Int?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                title = "Closed: ${isClosed ?: "..."}"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                title = "Raining: ${isRaining ?: "..."}"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .weight(1f),
               title = "PM2.5: ${pm25Level ?: "..."}",
               subtitle = "μg/m³"
           )

           Card(
               modifier = Modifier
                   .fillMaxWidth()
                   .weight(1f),
               title = "PM10: ${pm10Level ?: "..."}",
               subtitle = "μg/m³"
           )
        }
    }
}

@Composable
private fun ConfigurationSection(
    thresholds: Timestamped<DeviceThresholds>,
    updateThresholds: suspend (DeviceThresholds) -> Unit
) {
    var currentPM25Threshold by rememberSaveable {
        mutableStateOf(thresholds.value.pm25Threshold)
    }

    var currentPM10Threshold by rememberSaveable {
        mutableStateOf(thresholds.value.pm10Threshold)
    }

    val coroutineScope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(false) }

    val updateAllValues = {
        coroutineScope.launch {
            val new = thresholds.value.changed(
                newPM25 = currentPM25Threshold,
                newPM10 = currentPM10Threshold
            )

            updateThresholds(new)
            editing = false
        }
    }

    LaunchedEffect(thresholds) {
        if (!editing) {
            currentPM25Threshold = thresholds.value.pm25Threshold
            currentPM10Threshold = thresholds.value.pm10Threshold
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SliderWidget(
            title = "PM2.5 Threshold",
            currentValue = currentPM25Threshold,
            minValue = 0,
            maxValue = 500,
            setValue = {
                editing = true
                currentPM25Threshold = it
            },
            finishUpdate = { updateAllValues() },
            formatValue = { "$it μg/m³" }
        )

        SliderWidget(
            title = "PM10 Threshold",
            currentValue = currentPM10Threshold,
            minValue = 0,
            maxValue = 500,
            setValue = {
                editing = true
                currentPM10Threshold = it
            },
            finishUpdate = { updateAllValues() },
            formatValue = { "$it μg/m³" }
        )
    }
}

@Composable
private fun SliderWidget(
    title: String,
    maxValue: Int,
    currentValue: Int,
    minValue: Int = 0,
    setValue: (Int) -> Unit = {},
    finishUpdate: () -> Unit,
    formatValue: (Int) -> String = { it.toString() }
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, fontSize = 18.sp)
        Text(text = "Value: ${formatValue(currentValue)}", fontSize = 12.sp)

        Slider(
            value = currentValue.toFloat(),
            onValueChange = { setValue(it.toInt()) },
            onValueChangeFinished = { finishUpdate() },
            valueRange = minValue.toFloat().rangeTo(maxValue.toFloat())
        )
    }
}

@Composable
private fun SettingsSection(
    deviceViewModel: DeviceViewModel,
    device: Device,
    popUpToMain: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            device = device,
            onConfirmDelete = {
                coroutineScope.launch {
                    deviceViewModel.deleteDevice(device)
                    showDeleteDialog = false
                    popUpToMain()
                }
            },
            onCancel = {
                showDeleteDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                showDeleteDialog = true
            }
        ) {
            Text(text = "Delete Device")
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    device: Device,
    onConfirmDelete: (Device) -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        title = { Text(text = "Delete ${device.deviceName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Are you sure you want to delete device ${device.deviceName}?")
                Text(text = "This action is irreversible", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmDelete(device) }) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onCancel() }) {
                Text(text = "Cancel")
            }
        },
        onDismissRequest = { onCancel() }
    )
}