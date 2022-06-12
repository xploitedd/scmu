package xyz.xploited.scmumobile

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json
import xyz.xploited.scmumobile.database.MobileDatabase
import xyz.xploited.scmumobile.screen.device.DeviceRepo

class MobileApplication : Application() {

    private val db: MobileDatabase by lazy {
        Room.inMemoryDatabaseBuilder(
            applicationContext,
            MobileDatabase::class.java,
            // "scmu-mobile-db"
        ).fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('cozinha', '20:16:B9:43:24:83', 'test1_pk')")
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('quarto', '20:16:B9:43:24:84', 'test2_pk')")
                }
            })
            .build()
    }

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout)
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    val deviceRepo: DeviceRepo by lazy { DeviceRepo(db, httpClient) }

    val bluetoothAdapter: BluetoothAdapter by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

}