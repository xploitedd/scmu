package xyz.xploited.scmumobile.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import xyz.xploited.scmumobile.database.entities.Device

@Dao
interface DeviceDao {

    @Insert
    suspend fun insertDevice(device: Device): Long

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("select * from devices")
    fun getAllDevices(): Flow<List<Device>>

    @Query("select * from devices where id = :deviceId")
    suspend fun getDeviceById(deviceId: Int): Device?

    @Query("select * from devices where mac_address = :macAddress")
    suspend fun getDeviceByMac(macAddress: String): Device

}