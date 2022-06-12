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
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('test1', 'test1_ma', 'test1_pk')")
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('test2', 'test2_ma', 'test2_pk')")
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('test3', 'test3_ma', 'test3_pk')")
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('test4', 'test4_ma', 'test4_pk')")
                    db.execSQL("insert into devices (device_name, mac_address, public_key) values ('test5', 'test5_ma', 'test5_pk')")
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