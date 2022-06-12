package xyz.xploited.scmumobile.database

import androidx.room.Database
import androidx.room.RoomDatabase
import xyz.xploited.scmumobile.database.dao.DeviceDao
import xyz.xploited.scmumobile.database.entities.Device

@Database(entities = [Device::class], version = 1)
abstract class MobileDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

}