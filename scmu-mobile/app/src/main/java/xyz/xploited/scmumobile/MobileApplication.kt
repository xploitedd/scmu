package xyz.xploited.scmumobile

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

class MobileApplication : Application() {

    val bluetoothAdapter: BluetoothAdapter by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

}