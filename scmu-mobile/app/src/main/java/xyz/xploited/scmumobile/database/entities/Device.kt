package xyz.xploited.scmumobile.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "device_name") val deviceName: String,
    @ColumnInfo(name = "mac_address", index = true) val macAddress: String,
    @ColumnInfo(name = "public_key") val publicKey: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Device

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}